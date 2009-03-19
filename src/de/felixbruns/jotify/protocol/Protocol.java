package de.felixbruns.jotify.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
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
	/* Socket connection to spotify server */
	private Socket       socket;
	private InputStream  input;
	private OutputStream output;
	
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
				this.socket = new Socket(server.getHostname(), server.getPort());
				
				System.out.format("Connected to '%s'\n", server);
				
				/* Save server for later use. */
				this.server = server;
				
				break;
			}
			catch(IOException e){
				System.err.format("Error connecting to '%s': %s\n",server, e.getMessage());
			}
		}
		
		/* If connection was not established, return false. */
		if(this.socket == null){
			return false;
		}
		
		/* Get input and output stream. */
		try{
			this.input  = this.socket.getInputStream();
			this.output = this.socket.getOutputStream();
		}
		catch (IOException e){
			System.err.println("Error getting input or output streams: " + e.getMessage());
		}
		
		return true;
	}
	
	/* Disconnect from server */
	public void disconnect(){
		try{
			/* Close connection to server. */
			this.socket.close();
			
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
			2 + 2 + 1 + this.session.clientId.length + 4 + this.session.clientRandom.length +
			96 + 128 + 1 + this.session.username.length + 1 + 1 + 1
		);
		
		/* Append fields to buffer. */
		buffer.putShort((short)2); /* Version: 2 */
		buffer.putShort((short)0); /* Length (update later) */
		buffer.put(this.session.clientOs);
		buffer.put(this.session.clientId);
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
		
		/* Send it. */
		this.send(buffer.array());
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
		
		session.username = Arrays.copyOfRange(buffer, 0, usernameLength);
		
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
		session.dhServerPublicKey = DH.bytesToPublicKey(
			session.dhClientKeyPair.getPublicKey().getParams(),
			Arrays.copyOfRange(buffer, 0, 96)
		);
		
		/* Read server blob (256 bytes). */
		if((ret = this.receive(session.serverBlob, 0, 256)) != 256){
			System.err.println("Failed to read server blob.");
			
			return false;
		}
		
		/* Read salt (10 bytes). */
		if((ret = this.receive(session.salt, 0, 10)) != 10){
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
		
		/* Send it. */
		this.send(buffer.array());
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
	public void sendPacket(int command, byte[] payload){
		ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + payload.length);
		
		this.session.shannonSend.nonce(IntegerUtilities.toBytes(this.session.keySendIv));
		
		/* Build packet. */
		buffer.put((byte)command);
		buffer.putShort((short)payload.length);
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
		this.send(buffer.array());
		
		/* Increment IV. */
		this.session.keySendIv++;
	}
	
	/* Send a command without payload. */
	public void sendPacket(int command){
		this.sendPacket(command, new byte[]{});
	}
	
	/* Receive a packet (will be decrypted with stream cipher). */
	public boolean receivePacket(){
		byte[] bytes = new byte[3];
		int ret, command, payloadLength;
		
		/* Read header. */
		if((ret = this.receive(bytes, 3)) != 3){
			System.err.println("Failed to read header.");
			
			return false;
		}
		
		/* Save encrypted header for later. Please read below. */
		byte[] header = Arrays.copyOf(bytes, 3);
		
		/* Decrypt header. */
		this.session.shannonRecv.nonce(IntegerUtilities.toBytes(this.session.keyRecvIv));
		this.session.shannonRecv.decrypt(bytes);
		
		/* Get command from header. */
		command = bytes[0] & 0x00FF;
		
		/* Get length of payload. */
		payloadLength = ((bytes[1] & 0x00FF) << 8) | (bytes[2] & 0x00FF);
		
		/* Account for MAC. */
		payloadLength += 4;
		
		/* Allocate buffer. */
		bytes = new byte[payloadLength];
		
		if((ret = this.receive(bytes, payloadLength)) != payloadLength){
			System.err.format("Failed to read payload! %d != %d.", payloadLength, ret);
			
			return false;
		}
		
		/*
		 * Decrypting the remaining buffer should work, but it doesn't!
		 * And in my test case for the Shannon stream cipher, it works...
		 * To get around this problem, set nonce again, prepend those
		 * encrypted header bytes and successfully decrypt the whole thing.
		 */
		/* Start HACK. */
		this.session.shannonRecv.nonce(IntegerUtilities.toBytes(this.session.keyRecvIv));
		
		ByteBuffer buffer = ByteBuffer.allocate(3 + payloadLength);
		
		buffer.put(header);
		buffer.put(bytes);
		
		bytes = buffer.array();
		/* End HACK. */
		
		this.session.shannonRecv.decrypt(bytes);
		
		/* Remove Header and MAC bytes. */
		bytes = Arrays.copyOfRange(bytes, 3, payloadLength - 4 + 3);
		
		/* Increment IV. */
		this.session.keyRecvIv++;
		
		/* Fire events. */
		for(CommandListener listener : this.listeners){
			listener.commandReceived(command, bytes);
		}
		
		return true;
	}
	
	/* Send cache hash. */
	public void sendCacheHash(){
		this.sendPacket(Command.COMMAND_CACHEHASH, this.session.cacheHash);
	}
	
	/* Request ads. The response is GZIP compressed XML. */
	public void sendAdRequest(ChannelListener listener, int type){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Ad-Channel", Channel.Type.TYPE_AD, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 1);
		
		/* Append channel id and ad type. */
		buffer.putShort((short)channel.getId());
		buffer.put((byte)type);
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQUESTAD, buffer.array());
	}
	
	/* Request image using a 20 byte id. The response is a JPG. */
	public void sendImageRequest(ChannelListener listener, String id){
		/* Create channel and buffer. */
		Channel    channel = new Channel("Image-Channel", Channel.Type.TYPE_IMAGE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 20);
		
		/* Append channel id and image hash. */
		buffer.putShort((short)channel.getId());
		buffer.put(Hex.toBytes(id));
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_IMAGE, buffer.array());
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
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_SEARCH, buffer.array());
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
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQKEY, buffer.array());
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
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_GETSUBSTREAM, buffer.array());
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
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_BROWSE, buffer.array());
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
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PLAYLIST, buffer.array());
	}
	
	/* Ping reply (pong). */
	public void sendPong(){
		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		/* TODO: Append timestamp? */
		buffer.putInt(0x00000000);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PONG, buffer.array());
	}
	
	/* Send bytes. */
	private void send(byte[] buffer){
		try{
			this.output.write(buffer);
		}
		catch (IOException e){
			System.out.format("DEBUG: Error writing data to socket (%s).", e.getMessage());
		}
	}
	
	/* Receive a single byte. */
	private int receive(){
		try{
			return this.input.read();
		}
		catch (IOException e){
			System.out.format("DEBUG: Error reading data from socket (%s).", e.getMessage());
		}
		
		return -1;
	}
	
	/* Receive bytes. */
	private int receive(byte[] buffer, int len){
		return this.receive(buffer, 0, len);
	}
	
	/* Receive bytes. */
	private int receive(byte[] buffer, int off, int len){
		int read = 0, ret = -1;
		
		do{
			try{
				ret   = this.input.read(buffer, off, len - read);
				off  += ret;
				read += ret;
			}
			catch (IOException e){
				System.out.format("DEBUG: Error reading data from socket (%s).", e.getMessage());
			}
		} while(read < len);
		
		return read;
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
