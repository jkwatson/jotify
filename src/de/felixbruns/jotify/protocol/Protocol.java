package de.felixbruns.jotify.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.felixbruns.jotify.crypto.DH;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;
import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.IntegerUtilities;
import de.felixbruns.jotify.util.ServerLookup;
import de.felixbruns.jotify.util.ServerLookup.Server;

public class Protocol {
	/* Socket connection to Spotify server. */
	private SocketChannel channel;
	
	/* Current server and port */
	private Server server;
	
	/* Spotify session of this protocol instance. */
	private Session session;
	
	/* Protocol listeners. */
	private List<CommandListener> listeners;
	
	/* Create a new protocol object. */
	public Protocol(Session session){
		this.session   = session;
		this.listeners = new ArrayList<CommandListener>();
	}
	
	/* Connect to one of the spotify servers. */
	public boolean connect(){
		/* Lookup servers and try to connect, when connected to one of the servers, stop trying. */
		for(Server server : ServerLookup.lookupServers("_spotify-client._tcp.spotify.com")){
			try{
				/* Connect to server. */
				this.channel = SocketChannel.open(new InetSocketAddress(server.getHostname(), server.getPort()));
				
				/* Save server for later use. */
				this.server = server;
				
				break;
			}
			catch(IOException e){
				System.err.format("Error connecting to '%s': %s\n",server, e.getMessage());
			}
		}
		
		/* If connection was not established, return false. */
		if(this.channel == null){
			return false;
		}
		
		System.out.format("Connected to '%s'\n", this.server);
		
		return true;
	}
	
	/* Disconnect from server */
	public void disconnect(){
		try{
			/* Close connection to server. */
			this.channel.close();
			
			System.out.format("Disconnected from '%s'\n", this.server);
		}
		catch(IOException e){
			System.err.format("Error disconnecting from '%s': %s\n", this.server, e.getMessage());
		}
	}
	
	public void addListener(CommandListener listener){
		this.listeners.add(listener);
	}
	
	/* Send initial packet (key exchange). */
	public void sendInitialPacket(){
		ByteBuffer buffer = ByteBuffer.allocate(
			2 + 2 + 1 + 4 + 4 + 16 + 96 + 128 + 1 + this.session.username.length + 1 + 1 + 1
		);
		
		/* Append fields to buffer. */
		buffer.putShort((short)2); /* Version: 2 */
		buffer.putShort((short)0); /* Length (update later) */
		buffer.put(this.session.clientOs);
		buffer.put(this.session.clientId); /* 4 bytes */
		buffer.putInt(this.session.clientRevision);
		buffer.put(this.session.clientRandom); /* 16 bytes */
		buffer.put(this.session.dhClientKeyPair.getPublicKeyBytes()); /* 96 bytes */
		buffer.put(this.session.rsaClientKeyPair.getPublicKeyBytes()); /* 128 bytes */
		buffer.put((byte)this.session.username.length);
		buffer.put(this.session.username);
		buffer.put((byte)0x01);
		buffer.put((byte)0x40);
		
		/*
		 * Append zero or more random bytes.
		 * The first byte should be 1 + length.
		 */
		buffer.put((byte)0x01); /* Zero random bytes. */
		
		/* Update length byte. */
		buffer.putShort(2, (short)buffer.position());
		buffer.flip();
		
		/* Send it. */
		this.send(buffer);
	}
	
	/* Receive initial packet (key exchange). */
	public boolean receiveInitialPacket(){
		byte[] buffer = new byte[512];
		int ret, paddingLength, usernameLength;
		
		/* Read server random (first 2 bytes). */
		if((ret = this.receive(this.session.serverRandom, 0, 2)) == -1){
			System.err.println("Failed to read server random.");
			
			return false;
		}
		
		/* Check if we got a status message. */
		if(this.session.serverRandom[0] != 0x00 || ret != 2){
			/*
			 * Substatuses:
			 * 0x01    : Client upgrade required
			 * 0x03    : Non-existant user
			 * 0x09    : Your current country doesn't match that set in your profile.
			 * Default : Unknown error
			 */
			System.out.format("Status: %d, Substatus: %d => %s.\n",
				this.session.serverRandom[0], this.session.serverRandom[1],
				this.session.serverRandom[1] == 0x01 ?
						"Client upgrade required" : this.session.serverRandom[1] == 0x03 ?
								"Non-existant user" : "Unknown error"
			);
			
			/* If substatus is 'Client upgrade required', read upgrade URL. */
			if(this.session.serverRandom[1] == 0x01){
				if((ret = this.receive(buffer, 0x11a)) > 0){
					paddingLength = buffer[0x119];
					
					if((ret = this.receive(buffer, paddingLength)) > 0){
						System.out.println("Upgrade URL: " + new String(Arrays.copyOfRange(buffer, 0, ret)));
					}
				}
			}
			
			return false;
		}
		
		/* Read server random (next 14 bytes). */
		if((ret = this.receive(this.session.serverRandom, 2, 14)) != 14){
			System.err.println("Failed to read server random.");
			
			return false;
		}
		
		/* Read puzzle denominator. */
		if((this.session.puzzleDenominator = this.receive()) == -1){
			System.err.println("Failed to read puzzle denominator.");
			
			return false;
		}
		
		/* Read username length. */
		if((usernameLength = this.receive()) == -1){
			System.err.println("Failed to read username length.");
			
			return false;
		}
		
		/* Read username into buffer and copy it to 'session.username'. */
		if((ret = this.receive(buffer, usernameLength)) != usernameLength){
			System.err.println("Failed to read username.");
			
			return false;
		}
		
		this.session.username = Arrays.copyOfRange(buffer, 0, usernameLength);
		
		/* Read server public key (Diffie Hellman key exchange). */
		if((ret = this.receive(buffer, 96)) != 96){
			System.err.println("Failed to read server public key.");
			
			return false;
		}
		
		/* 
		 * Convert key, which is in raw byte form to a DHPublicKey
		 * using the DHParameterSpec (for P and G values) of our
		 * public key. Y value is taken from raw bytes.
		 */
		this.session.dhServerPublicKey = DH.bytesToPublicKey(
				this.session.dhClientKeyPair.getPublicKey().getParams(),
			Arrays.copyOfRange(buffer, 0, 96)
		);
		
		/* Read server blob (256 bytes). */
		if((ret = this.receive(this.session.serverBlob, 0, 256)) != 256){
			System.err.println("Failed to read server blob.");
			
			return false;
		}
		
		/* Read salt (10 bytes). */
		if((ret = this.receive(this.session.salt, 0, 10)) != 10){
			System.err.println("Failed to read salt.");
			
			return false;
		}
		
		/* Read padding length (1 byte). */
		if((paddingLength = this.receive()) == -1){
			System.err.println("Failed to read paddling length.");
			
			return false;
		}
		
		/* Check if padding length is valid. */
		if(paddingLength <= 0){
			System.err.println("Padding length is negative or zero.");
			
			return false;
		}
		
		/* Includes itself. */
		paddingLength--;
		
		/* Read padding. */
		if((ret = this.receive(buffer, paddingLength)) != paddingLength){
			System.err.println("Failed to read padding.");
			
			return false;
		}
		
		/* Successfully read everything. */
		return true;
	}
	
	/* Send authentication packet (puzzle solution, HMAC). */
	public void sendAuthenticationPacket(){
		ByteBuffer buffer = ByteBuffer.allocate(8 + 20 + 1 + 1);
		
		/* Append fields to buffer. */
		buffer.put(this.session.puzzleSolution); /* 8 bytes */
		buffer.put(this.session.authHmac); /* 20 bytes */
		buffer.put((byte)0x00); /* Unknown. */
		/* Payload length + junk byte. Payload can be anything and doesn't appear to be used. */
		buffer.put((byte)0x01); /* Zero payload. */
		buffer.flip();
		
		/* Send it. */
		this.send(buffer);
	}
	
	/* Receive authentication packet (status). */
	public boolean receiveAuthenticationPacket(){
		byte[] buffer = new byte[512];
		int payloadLength;
		
		/* Read status and length. */
		if(this.receive(buffer, 2) != 2){
			System.err.println("Failed to read status and length bytes.");
			
			return false;
		}
		
		/* Check status. */
		if(buffer[0] != 0x00){
			System.err.format("Authentication failed with error 0x%02x, bad password?\n", buffer[1]);
			
			return false;
		}
		
		/* Check payload length. AND with 0x00FF so we don't get a negative integer. */
		if((payloadLength = 0x00FF & buffer[1]) <= 0){
			System.err.println("Payload length is negative or zero.");
			
			return false;
		}
		
		/* Includes itself. */
		payloadLength--;
		
		/* Read payload. */
		if(this.receive(buffer, payloadLength) != payloadLength){
			System.err.println("Failed to read payload.");
			
			return false;
		}
		
		return true;
	}
	
	/* Send command with payload (will be encrypted with stream cipher). */
	public void sendPacket(int command, ByteBuffer payload){
		ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + payload.remaining());
		
		this.session.shannonSend.nonce(IntegerUtilities.toBytes(this.session.keySendIv));
		
		/* Build packet. */
		buffer.put((byte)command);
		buffer.putShort((short)payload.remaining());
		buffer.put(payload);
		
		byte[] bytes = buffer.array();
		byte[] mac   = new byte[4];
		
		/* Encrypt packet and get MAC. */
		this.session.shannonSend.encrypt(bytes);
		this.session.shannonSend.finish(mac);
		
		buffer = ByteBuffer.allocate(buffer.position() + 4);
		buffer.put(bytes);
		buffer.put(mac);
		buffer.flip();
		
		/* Send encrypted packet. */
		this.send(buffer);
		
		/* Increment IV. */
		this.session.keySendIv++;
	}
	
	/* Send a command without payload. */
	public void sendPacket(int command){
		this.sendPacket(command, ByteBuffer.allocate(0));
	}
	
	/* Receive a packet (will be decrypted with stream cipher). */
	public boolean receivePacket(){
		byte[] header = new byte[3];
		int command, payloadLength, headerLength = 3, macLength = 4;
		
		/* Read header. */
		if(this.receive(header, headerLength) != headerLength){
			System.err.println("Failed to read header.");
			
			return false;
		}
		
		/* Save encrypted header for later. Please read below. */
		byte[] rawHeader = Arrays.copyOf(header, headerLength);
		
		/* Decrypt header. */
		this.session.shannonRecv.nonce(IntegerUtilities.toBytes(this.session.keyRecvIv));
		this.session.shannonRecv.decrypt(header);
		
		/* Get command and payload length from header. */
		ByteBuffer headerBuffer = ByteBuffer.wrap(header);
		
		command       = headerBuffer.get() & 0xff;
		payloadLength = headerBuffer.getShort() & 0xffff;
		
		/* Allocate buffer. Account for MAC. */
		byte[] bytes      = new byte[headerLength + payloadLength + macLength];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.put(rawHeader);
		buffer.limit(headerLength + payloadLength);
		
		try{
			for(int n = payloadLength, r; n > 0 && (r = this.channel.read(buffer)) > 0; n -= r);
		}
		catch(IOException e){
			System.err.format("Failed to read payload. (%s)\n", e.getMessage());
		}
		
		buffer.limit(headerLength + payloadLength + macLength);
		
		try{
			for(int n = macLength, r; n > 0 && (r = this.channel.read(buffer)) > 0; n -= r);
		}
		catch(IOException e){
			System.err.format("Failed to read MAC. (%s)\n", e.getMessage());
		}
		
		/*
		 * Decrypting the remaining buffer should work, but it doesn't!
		 * And in my test case for the Shannon stream cipher, it works...
		 * To get around this problem, set nonce again, prepend those
		 * encrypted header bytes and successfully decrypt the whole thing.
		 */
		this.session.shannonRecv.nonce(IntegerUtilities.toBytes(this.session.keyRecvIv));
		this.session.shannonRecv.decrypt(bytes);
		
		/* Remove Header and MAC bytes. */
		byte[] payload = new byte[payloadLength];
		
		buffer.flip();
		buffer.position(headerLength);
		buffer.get(payload);
		
		/* Increment IV. */
		this.session.keyRecvIv++;
		
		/* Fire events. */
		for(CommandListener listener : this.listeners){
			listener.commandReceived(command, payload);
		}
		
		return true;
	}
	
	/* Send cache hash. */
	public void sendCacheHash(){
		ByteBuffer buffer = ByteBuffer.allocate(20);
		
		buffer.put(this.session.cacheHash);
		buffer.flip();
		
		this.sendPacket(Command.COMMAND_CACHEHASH, buffer);
	}
	
	/* Request ads. The response is GZIP compressed XML. */
	public void sendAdRequest(ChannelListener listener, int type){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Ad-Channel", Channel.Type.TYPE_AD, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 1);
		
		/* Append channel id and ad type. */
		buffer.putShort((short)channel.getId());
		buffer.put((byte)type);
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQUESTAD, buffer);
	}
	
	/* Request image using a 20 byte id. The response is a JPG. */
	public void sendImageRequest(ChannelListener listener, String id){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Image-Channel", Channel.Type.TYPE_IMAGE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 20);
		
		/* Append channel id and image hash. */
		buffer.putShort((short)channel.getId());
		buffer.put(Hex.toBytes(id));
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_IMAGE, buffer);
	}
	
	/* Search music. The response comes as GZIP compressed XML. */
	public void sendSearchQuery(ChannelListener listener, String query){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Search-Channel", Channel.Type.TYPE_SEARCH, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 4 + 4 + 2 + 1 + query.getBytes().length);
		
		/* Append channel id, some values, query length and query. */
		buffer.putShort((short)channel.getId());
		buffer.putInt(0x00000000);
		buffer.putInt(0xffffffff);
		buffer.putShort((short)0x0000);
		buffer.put((byte)query.length());
		buffer.put(query.getBytes());
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_SEARCH, buffer);
	}
	
	/* Notify server we're going to play. */
	public void sendTokenNotify(){
		this.sendPacket(Command.COMMAND_TOKENNOTIFY);
	}
	
	/* Request AES key for a track. */
	public void sendAesKeyRequest(ChannelListener listener, Track track){
		/* Create channel and buffer. */
		Channel    channel = new Channel("AES-Key-Channel", Channel.Type.TYPE_AESKEY, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(20 + 16 + 2 + 2);
		
		/* Request the AES key for this file by sending the file id and track id. */
		buffer.put(Hex.toBytes(track.getFiles().get(0))); /* 20 bytes */
		buffer.put(Hex.toBytes(track.getId())); /* 16 bytes */
		buffer.putShort((short)0x0000);
		buffer.putShort((short)channel.getId());
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQKEY, buffer);
	}
	
	/* A demo wrapper for playing a track. */
	public void sendPlayRequest(ChannelListener listener, Track track){
		/* 
		 * Notify the server about our intention to play music, there by allowing
		 * it to request other players on the same account to pause.
		 * 
		 * Yet another client side restriction to annony those who share their
		 * Spotify account with not yet invited friends. And as a bonus it won't
		 * play commercials and waste bandwidth in vain.
		 */
		this.sendPacket(Command.COMMAND_REQUESTPLAY);
		this.sendAesKeyRequest(listener, track);
	}
	
	/*
	 * Request a part of the encrypted file from the server.
	 * 
	 * The data should be decrypted using AES key in CTR mode
	 * with AES key provided and a static IV, incremented for
	 * each 16 byte data processed.
	 */
	public void sendSubstreamRequest(ChannelListener listener, Track track, int offset, int length){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Substream-Channel", Channel.Type.TYPE_SUBSTREAM, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 2 + 2 + 2 + 2 + 4 + 20 + 4 + 4);
		
		/* Append channel id. */
		buffer.putShort((short)channel.getId());
		
		/* Unknown 10 bytes. */
		buffer.putShort((short)0x0800);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x4e20);
		
		/* Unknown (static value) */
		buffer.putInt(200 * 1000);
		
		/* 20 bytes file id. */
		buffer.put(Hex.toBytes(track.getFiles().get(0)));
		
		if(offset % 4096 != 0 || length % 4096 != 0){
			throw new IllegalArgumentException("Offset and length need to be a multiple of 4096.");	
		}
		
		offset >>= 2;
		length >>= 2;
		
		/* Append offset and length. */
		buffer.putInt(offset);
		buffer.putInt(offset + length);
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_GETSUBSTREAM, buffer);
	}
	
	/*
	 * Get metadata for an artist (type = 1), album (type = 2) or a
	 * list of tracks (type = 3). The response comes as compressed XML.
	 */
	public void sendBrowseRequest(ChannelListener listener, int type, Collection<String> ids){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Browse-Channel", Channel.Type.TYPE_BROWSE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 1 + ids.size() * 20 + ((type == 1 || type == 2)?4:0));
		
		/* Check arguments. */
		if(type != 1 && type != 2 && type != 3){
			throw new IllegalArgumentException("Type needs to be 1, 2 or 3.");
		}
		else if((type == 1 && type == 2) && ids.size() != 1){
			throw new IllegalArgumentException("Types 1 and 2 only accept a single id.");
		}
		
		/* Append channel id and type. */
		buffer.putShort((short)channel.getId());
		buffer.put((byte)type);
		
		/* Append id's. */
		for(String id : ids){
			buffer.put(Hex.toBytes(id));
		}
		
		/* Append zero. */
		if(type == 1 || type == 2){
			buffer.putInt(0);
		}
		
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_BROWSE, buffer);
	}
	
	/* Browse with only one id. */
	public void sendBrowseRequest(ChannelListener listener, int type, String id){
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(id);
		
		this.sendBrowseRequest(listener, type, list);
	}
	
	/* Request playlist details. The response comes as plain XML. */
	public void sendPlaylistRequest(ChannelListener listener, byte[] playlistId, int unknown){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Playlist-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 17 + 4 + 4 + 4 + 1);
		
		/* Append channel id, playlist id and some bytes... */
		buffer.putShort((short)channel.getId());
		buffer.put(playlistId); /* 17 bytes */
		buffer.putInt(unknown);
		buffer.putInt(0x00000000);
		buffer.putInt(0xffffffff);
		buffer.put((byte)0x01);
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PLAYLIST, buffer);
	}
	
	/* Ping reply (pong). */
	public void sendPong(){
		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		/* TODO: Append timestamp? */
		buffer.putInt(0x00000000);
		buffer.flip();
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PONG, buffer);
	}
	
	/* Send bytes. */
	private void send(ByteBuffer buffer){
		try{
			this.channel.write(buffer);
		}
		catch (IOException e){
			System.out.format("Error writing data to socket (%s).", e.getMessage());
		}
	}
	
	/* Receive a single byte. */
	private int receive(){
		ByteBuffer buffer = ByteBuffer.allocate(1);
		
		try{
			this.channel.read(buffer);
			
			buffer.flip();
			
			return buffer.get() & 0xff;
		}
		catch(IOException e){
			System.out.format("Error reading data from socket (%s).", e.getMessage());
		}
		
		return -1;
	}
	
	/* Receive bytes. */
	private int receive(byte[] buffer, int len){
		return this.receive(buffer, 0, len);
	}
	
	/* Receive bytes. */
	private int receive(byte[] bytes, int off, int len){
		ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);
		int n = 0;
		
		try{
			for(int r; n < len && (r = this.channel.read(buffer)) > 0; n += r);
		}
		catch(IOException e){
			System.out.format("Error reading data from socket (%s).\n", e.getMessage());
			
			return -1;
		}
		
		return n;
	}
	
	/*private void logPacket(byte[] bytes, String sr, String hp, int k){
		String file = String.format("C:\\debug\\spotify.%s-%s.%d", sr, hp, k);
		try{
			FileOutputStream fos = new FileOutputStream(file);
			
			fos.write(bytes);
			fos.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}*/
}
