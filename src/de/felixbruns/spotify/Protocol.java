package de.felixbruns.spotify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.felixbruns.spotify.crypto.DH;
import de.felixbruns.spotify.util.Buffer;
import de.felixbruns.spotify.util.Hex;
import de.felixbruns.spotify.util.IntegerUtilities;
import de.felixbruns.spotify.util.ServerLookup;
import de.felixbruns.spotify.util.ServerLookup.Server;

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
		Buffer buffer = new Buffer();
		
		/* Append fields to buffer. */
		buffer.appendShort((short)2); /* Version: 2 */
		buffer.appendShort((short)0); /* Length (update later) */
		buffer.appendByte(this.session.clientOs);
		buffer.appendBytes(this.session.clientId);
		buffer.appendInt(this.session.clientRevision);
		buffer.appendBytes(this.session.clientRandom); /* 16 bytes */
		buffer.appendBytes(this.session.dhClientKeyPair.getPublicKeyBytes()); /* 96 bytes */
		buffer.appendBytes(this.session.rsaClientKeyPair.getPublicKeyBytes()); /* 128 bytes */
		buffer.appendByte((byte)this.session.username.length);
		buffer.appendBytes(this.session.username);
		buffer.appendByte((byte)0x01);
		buffer.appendByte((byte)0x40);
		
		/*
		 * Append zero or more random bytes.
		 * The first byte should be 1 + length.
		 */
		buffer.appendByte((byte)0x01); /* Zero random bytes. */
		
		/* Update length byte. */
		buffer.setShort(2, (short)buffer.position());
		
		/* Send it. */
		this.send(buffer.getBytes());
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
		Buffer buffer = new Buffer();
		
		/* Append fields to buffer. */
		buffer.appendBytes(this.session.puzzleSolution);
		buffer.appendBytes(this.session.authHmac);
		buffer.appendByte((byte)0x00); /* Unknown. */
		/* Payload length + junk byte. Payload can be anything and doesn't appear to be used. */
		buffer.appendByte((byte)0x01); /* Zero payload. */
		
		/* Send it. */
		this.send(buffer.getBytes());
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
		Buffer buffer = new Buffer(3 + payload.length);
		
		this.session.shannonSend.nonce(IntegerUtilities.toBytes(this.session.keySendIv));
		
		/* Build packet. */
		buffer.appendByte((byte)command);
		buffer.appendShort((short)payload.length);
		buffer.appendBytes(payload);
		
		byte[] bytes = buffer.getBytes();
		byte[] mac   = new byte[4];
		
		/* Encrypt packet and get MAC. */
		this.session.shannonSend.encrypt(bytes);
		this.session.shannonSend.finish(mac);
		
		buffer = new Buffer();
		buffer.appendBytes(bytes);
		buffer.appendBytes(mac);
		
		/* Send encrypted packet. */
		this.send(buffer.getBytes());
		
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
		
		Buffer buffer = new Buffer(payloadLength + 3);
		
		buffer.appendBytes(header);
		buffer.appendBytes(bytes);
		
		bytes = buffer.getBytes();
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
		Buffer buffer = new Buffer();
		
		buffer.appendBytes(this.session.cacheHash);
		
		this.sendPacket(Command.COMMAND_CACHEHASH, buffer.getBytes());
	}
	
	/* Request ads. The response is GZIP compressed XML. */
	public void sendAdRequest(ChannelListener listener, int type){
		/* Create channel and buffer. */
		Channel channel = new Channel("Ad-Channel", Channel.Type.TYPE_AD, listener);
		Buffer  buffer  = new Buffer();
		
		/* Append channel id and ad type. */
		buffer.appendShort((short)channel.getId());
		buffer.appendByte((byte)type);
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQUESTAD, buffer.getBytes());
	}
	
	/* Request image using a 20 byte hash. The response is a JPG. */
	public void sendImageRequest(ChannelListener listener, String id){
		/* Create channel and buffer. */
		Channel channel = new Channel("Image-Channel", Channel.Type.TYPE_IMAGE, listener);
		Buffer  buffer  = new Buffer();
		
		/* Append channel id and image hash. */
		buffer.appendShort((short)channel.getId());
		buffer.appendBytes(Hex.toBytes(id));
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_IMAGE, buffer.getBytes());
	}
	
	/* Search music. The response comes as GZIP compressed XML. */
	public void sendSearchQuery(ChannelListener listener, String query){
		/* Create channel and buffer. */
		Channel channel = new Channel("Search-Channel", Channel.Type.TYPE_SEARCH, listener);
		Buffer  buffer  = new Buffer();
		
		/* Append channel id, some values, query length and query. */
		buffer.appendShort((short)channel.getId());
		buffer.appendInt(0x00000000);
		buffer.appendInt(0xffffffff);
		buffer.appendShort((short)0x0000);
		buffer.appendByte((byte)query.length());
		buffer.appendBytes(query.getBytes());
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_SEARCH, buffer.getBytes());
	}
	
	/* Notify server we're going to play. */
	public void sendTokenNotify(){
		this.sendPacket(Command.COMMAND_TOKENNOTIFY);
	}
	
	/* Request AES key for a track. */
	public void sendAesKeyRequest(ChannelListener listener, Track track){
		/* Create channel and buffer. */
		Channel channel = new Channel("AES-Key-Channel", Channel.Type.TYPE_AESKEY, listener);
		Buffer  buffer  = new Buffer();
		
		/* Request the AES key for this file by sending the file id and track id. */
		buffer.appendBytes(Hex.toBytes(track.getFiles().get(0))); /* 20 bytes */
		buffer.appendBytes(Hex.toBytes(track.getId())); /* 16 bytes */
		buffer.appendShort((short)0x0000);
		buffer.appendShort((short)channel.getId());
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQKEY, buffer.getBytes());
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
		Channel channel = new Channel("Substream-Channel", Channel.Type.TYPE_SUBSTREAM, listener);
		Buffer  buffer  = new Buffer();
		
		/* Append channel id. */
		buffer.appendShort((short)channel.getId());
		
		/* Unknown 10 bytes. */
		buffer.appendShort((short)0x0800);
		buffer.appendShort((short)0x0000);
		buffer.appendShort((short)0x0000);
		buffer.appendShort((short)0x0000);
		buffer.appendShort((short)0x4e20);
		
		/* Unknown (static value) */
		buffer.appendInt(200 * 1000);
		
		/* 20 bytes file id. */
		buffer.appendBytes(Hex.toBytes(track.getFiles().get(0)));
		
		if(offset % 4096 != 0 || length % 4096 != 0){
			throw new IllegalArgumentException("Offset and length need to be a multiple of 4096.");	
		}
		
		offset >>= 2;
		length >>= 2;
		
		/* Append offset and length. */
		buffer.appendInt(offset);
		buffer.appendInt(offset + length);
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_GETSUBSTREAM, buffer.getBytes());
	}
	
	/*
	 * Get metadata for an artist (type = 1), album (type = 2) or a
	 * list of tracks (type = 3). The response comes as compressed XML.
	 */
	public void sendBrowseRequest(ChannelListener listener, int type, Collection<String> ids){
		/* Create channel and buffer. */
		Channel channel = new Channel("Browse-Channel", Channel.Type.TYPE_BROWSE, listener);
		Buffer  buffer  = new Buffer();
		
		/* Check arguments. */
		if(type != 1 && type != 2 && type != 3){
			throw new IllegalArgumentException("Type needs to be 1, 2 or 3.");
		}
		else if((type == 1 && type == 2) && ids.size() != 1){
			throw new IllegalArgumentException("Types 1 and 2 only accept a single id.");
		}
		
		/* Append channel id and type. */
		buffer.appendShort((short)channel.getId());
		buffer.appendByte((byte)type);
		
		/* Append id's. */
		for(String id : ids){
			buffer.appendBytes(Hex.toBytes(id));
		}
		
		/* Append zero. */
		if(type == 1 || type == 2){
			buffer.appendInt(0);
		}
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_BROWSE, buffer.getBytes());
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
		Channel channel = new Channel("Playlist-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		Buffer  buffer  = new Buffer();
		
		/* Append channel id, playlist id and some bytes... */
		buffer.appendShort((short)channel.getId());
		buffer.appendBytes(playlistId); /* 17 bytes */
		buffer.appendInt(unknown);
		buffer.appendInt(0x00000000);
		buffer.appendInt(0xffffffff);
		buffer.appendByte((byte)0x01);
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PLAYLIST, buffer.getBytes());
	}
	
	/* Ping reply (pong). */
	public void sendPong(){
		Buffer buffer = new Buffer();
		
		/* TODO: Append timestamp? */
		buffer.appendInt(0x00000000);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PONG, buffer.getBytes());
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
