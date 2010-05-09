package de.felixbruns.jotify.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.felixbruns.jotify.crypto.DH;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;
import de.felixbruns.jotify.util.DNS;
import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.IntegerUtilities;

public class Protocol {
	/* Socket connection to Spotify server. */
	private SocketChannel channel;
	
	/* Current server and port */
	private InetSocketAddress server;
	
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
	public void connect() throws ConnectionException {
		/* Lookup servers via DNS SRV query. */
		List<InetSocketAddress> servers = DNS.lookupSRV("_spotify-client._tcp.spotify.com");
		
		/* Add fallback servers if others don't work. */
		servers.add(new InetSocketAddress("ap.spotify.com", 4070));
		servers.add(new InetSocketAddress("ap.spotify.com", 80));
		servers.add(new InetSocketAddress("ap.spotify.com", 443));
		
		/* Try to connect to each server, stop trying when connected. */
		for(InetSocketAddress server : servers){
			try{
				/* Try to connect to current server with a timeout of 1 second. */
				this.channel = SocketChannel.open();
				this.channel.socket().connect(server, 1000);
				
				/* Save server for later use. */
				this.server = server;
				
				break;
			}
			catch(Exception e){
				System.err.println("Error connecting to '" + server + "': " + e.getMessage());
			}
		}
		
		/* If connection was not established, return false. */
		if(!this.channel.isConnected()){
			throw new ConnectionException("Error connecting to any server!");
		}
		
		System.out.format("Connected to '%s'\n", this.server);
	}
	
	/* Disconnect from server */
	public void disconnect() throws ConnectionException {
		try{
			/* Close connection to server. */
			this.channel.close();
			
			System.out.format("Disconnected from '%s'\n", this.server);
		}
		catch(IOException e){
			throw new ConnectionException("Error disconnecting from '" + this.server + "'!", e);
		}
	}
	
	public void addListener(CommandListener listener){
		this.listeners.add(listener);
	}
	
	/* Send initial packet (key exchange). */
	public void sendInitialPacket() throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(
			2 + 2 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 16 + 96 + 128 + 1 + 1 + 2 + 0 + this.session.username.length + 1
		);
		
		/* Append fields to buffer. */
		buffer.putShort((short)3); /* Version 3 */
		buffer.putShort((short)0); /* Length (update later) */
		buffer.putInt(this.session.clientOs);
		buffer.putInt(0x00000000); /* Unknown */
		buffer.putInt(this.session.clientRevision);
		buffer.putInt(0x1541ECD0); /* Windows: 0x1541ECD0, Mac OSX: 0x00000000 */
		buffer.putInt(0x01000000); /* Windows: 0x01000000, Mac OSX: 0x01040000 */
		buffer.putInt(this.session.clientId); /* 4 bytes, Windows: 0x010B0029, Mac OSX: 0x026A0200 */
		buffer.putInt(0x00000001); /* Unknown */
		buffer.put(this.session.clientRandom); /* 16 bytes */
		buffer.put(this.session.dhClientKeyPair.getPublicKeyBytes()); /* 96 bytes */
		buffer.put(this.session.rsaClientKeyPair.getPublicKeyBytes()); /* 128 bytes */
		buffer.put((byte)0); /* Random length */
		buffer.put((byte)this.session.username.length); /* Username length */
		buffer.putShort((short)0x0100); /* Unknown */
		/* Random bytes here... */
		buffer.put(this.session.username);
		buffer.put((byte)0x5F);/* Minor protocol version. */
		
		/* Update length byte. */
		buffer.putShort(2, (short)buffer.position());
		buffer.flip();
		
		/* Save initial client packet for auth hmac generation. */
		this.session.initialClientPacket = new byte[buffer.remaining()];
		
		buffer.get(this.session.initialClientPacket);
		buffer.flip();
		
		/* Send it. */
		this.send(buffer);
	}
	
	/* Receive initial packet (key exchange). */
	public void receiveInitialPacket() throws ProtocolException {
		byte[] buffer = new byte[512];
		int ret, paddingLength, usernameLength;
		
		/* Save initial server packet for auth hmac generation. 1024 bytes should be enough. */
		ByteBuffer serverPacketBuffer = ByteBuffer.allocate(1024);
		
		/* Read server random (first 2 bytes). */
		if((ret = this.receive(this.session.serverRandom, 0, 2)) != 2){
			throw new ProtocolException("Failed to read server random.");
		}
		
		/* Check if we got a status message. */
		if(this.session.serverRandom[0] != 0x00){
			/*
			 * Substatuses:
			 * 0x01    : Client upgrade required.
			 * 0x03    : Nonexistent user.
			 * 0x04    : Account has been disabled.
			 * 0x06    : You need to complete your account details.
			 * 0x09    : Your current country doesn't match that set in your profile.
			 * Default : Unknown error
			 */
			StringBuilder message = new StringBuilder(255);
			
			/* Check substatus and set message. */
			switch(this.session.serverRandom[1]){
				case 0x01:
					message.append("Client upgrade required: ");
					break;
				case 0x03:
					message.append("Nonexistent user.");
					break;
				case 0x04:
					message.append("Account has been disabled.");
					break;
				case 0x06:
					message.append("You need to complete your account details.");
					break;
				case 0x09:
					message.append("Your current country doesn't match that set in your profile.");
					break;
				default:
					message.append("Unknown error.");
					break;
			}
			
			/* If substatus is 'Client upgrade required', update client revision. */
			if(this.session.serverRandom[1] == 0x01){
				if((ret = this.receive(buffer, 0x11a)) > 0){
					paddingLength = buffer[0x119] & 0xFF;
					
					if((ret = this.receive(buffer, paddingLength)) > 0){
						String  msg     = new String(Arrays.copyOfRange(buffer, 0, ret));
						Pattern pattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.[0-9a-z]+");
						Matcher matcher = pattern.matcher(msg);
						
						/* Update client revision. */
						if(matcher.find()){
							/* 
							 * 0x0266EF51: 40.300.369 -> 0.4.3.369.gd0ec4115
							 * 0x0266EF5C: 40.300.380 -> 0.4.3.380.g88163066
							 * 0x0266EF5C: 40.300.383 -> 0.4.3.380.g278a6e51
							 * 
							 * major * 1000000000 (???) + minor * 10000000 + maintenance * 100000 + build.
							 */
							this.session.clientRevision  = Integer.parseInt(matcher.group(2)) * 10000000;
							this.session.clientRevision += Integer.parseInt(matcher.group(3)) * 100000;
							this.session.clientRevision += Integer.parseInt(matcher.group(4));
						}
						
						message.append(msg);
					}
				}
			}
			
			throw new ProtocolException(message.toString());
		}
		
		/* Read server random (next 14 bytes). */
		if((ret = this.receive(this.session.serverRandom, 2, 14)) != 14){
			throw new ProtocolException("Failed to read server random.");
		}
		
		/* Save server random to packet buffer. */
		serverPacketBuffer.put(this.session.serverRandom);
		
		/* Read server public key (Diffie Hellman key exchange). */
		if((ret = this.receive(buffer, 96)) != 96){
			throw new ProtocolException("Failed to read server public key.");
		}
		
		/* Save DH public key to packet buffer. */
		serverPacketBuffer.put(buffer, 0, 96);
		
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
			throw new ProtocolException("Failed to read server blob.");
		}
		
		/* Save RSA signature to packet buffer. */
		serverPacketBuffer.put(this.session.serverBlob);
		
		/* Read salt (10 bytes). */
		if((ret = this.receive(this.session.salt, 0, 10)) != 10){
			throw new ProtocolException("Failed to read salt.");
		}
		
		/* Save salt to packet buffer. */
		serverPacketBuffer.put(this.session.salt);
		
		/* Read padding length (1 byte). */
		if((paddingLength = this.receive()) == -1){
			throw new ProtocolException("Failed to read paddling length.");
		}
		
		/* Save padding length to packet buffer. */
		serverPacketBuffer.put((byte)paddingLength);
		
		/* Check if padding length is valid. */
		if(paddingLength <= 0){
			throw new ProtocolException("Padding length is negative or zero.");
		}
		
		/* Read username length. */
		if((usernameLength = this.receive()) == -1){
			throw new ProtocolException("Failed to read username length.");
		}
		
		/* Save username length to packet buffer. */
		serverPacketBuffer.put((byte)usernameLength);
		
		/* Read lengths of puzzle challenge and unknown fields */
		this.receive(buffer, 8);
		
		/* Save bytes to packet buffer. */
		serverPacketBuffer.put(buffer, 0, 8);
		
		/* Get lengths of puzzle challenge and unknown fields.  */
		ByteBuffer dataBuffer     = ByteBuffer.wrap(buffer, 0, 8);
		int puzzleChallengeLength = dataBuffer.getShort();
		int unknownLength1        = dataBuffer.getShort();
		int unknownLength2        = dataBuffer.getShort();
		int unknownLength3        = dataBuffer.getShort();
		
		/* Read padding. */
		if((ret = this.receive(buffer, paddingLength)) != paddingLength){
			throw new ProtocolException("Failed to read padding.");
		}
		
		/* Save padding (random bytes) to packet buffer. */
		serverPacketBuffer.put(buffer, 0, paddingLength);
		
		/* Read username into buffer and copy it to 'session.username'. */
		if((ret = this.receive(buffer, usernameLength)) != usernameLength){
			throw new ProtocolException("Failed to read username.");
		}
		
		/* Save username to packet buffer. */
		serverPacketBuffer.put(buffer, 0, usernameLength);
		
		/* Save username to session. */
		this.session.username = Arrays.copyOfRange(buffer, 0, usernameLength);
		
		/* Receive puzzle challenge and unknown bytes (more puzzle lengths, seem to be always zero). */
		this.receive(buffer,                                                       0, puzzleChallengeLength);
		this.receive(buffer,                                   puzzleChallengeLength, unknownLength1);
		this.receive(buffer,                  puzzleChallengeLength + unknownLength1, unknownLength2);
		this.receive(buffer, puzzleChallengeLength + unknownLength1 + unknownLength2, unknownLength3);
		
		/* Save to packet buffer. */
		serverPacketBuffer.put(buffer, 0, puzzleChallengeLength + unknownLength1 + unknownLength2 + unknownLength3);
		serverPacketBuffer.flip();
		
		/* Write data from packet buffer to byte array. */
		this.session.initialServerPacket = new byte[serverPacketBuffer.remaining()];
		
		serverPacketBuffer.get(this.session.initialServerPacket);
		
		/* Wrap buffer in order to get values. */
		dataBuffer = ByteBuffer.wrap(buffer, 0, puzzleChallengeLength + unknownLength1 + unknownLength2 + unknownLength3);
		
		/* Get puzzle denominator and magic. */
		if(dataBuffer.get() == 0x01){ /* 0x01: SHA-1 puzzle, 0x00: no puzzle. */
			this.session.puzzleDenominator = dataBuffer.get();
			this.session.puzzleMagic       = dataBuffer.getInt();
		}
		else{
			throw new ProtocolException("Unexpected puzzle challenge.");
		}
	}
	
	/* Send authentication packet (puzzle solution, HMAC). */
	public void sendAuthenticationPacket() throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(20 + 1 + 1 + 4 + 2 + 15 + this.session.puzzleSolution.length);
		
		/* Append fields to buffer. */
		buffer.put(this.session.authHmac); /* 20 bytes */
		buffer.put((byte)0); /* Random data length */
		buffer.put((byte)0); /* Unknown. */
		buffer.putShort((short)this.session.puzzleSolution.length);
		buffer.putInt(0x0000000); /* Unknown. */
		/* Random bytes here... */
		buffer.put(this.session.puzzleSolution); /* 8 bytes */
		buffer.flip();
		
		/* Send it. */
		this.send(buffer);
	}
	
	/* Receive authentication packet (status). */
	public void receiveAuthenticationPacket() throws ProtocolException {
		byte[] buffer = new byte[512];
		int payloadLength;
		
		/* Read status and length. */
		if(this.receive(buffer, 2) != 2){
			throw new ProtocolException("Failed to read status and length bytes.");
		}
		
		/* Check status. */
		if(buffer[0] != 0x00){
			throw new ProtocolException("Authentication failed!");
		}
		
		/* Check payload length. AND with 0x00FF so we don't get a negative integer. */
		if((payloadLength = buffer[1] & 0xFF) <= 0){
			throw new ProtocolException("Payload length is negative or zero.");
		}
		
		/* Read payload. */
		if(this.receive(buffer, payloadLength) != payloadLength){
			throw new ProtocolException("Failed to read payload.");
		}
	}
	
	/* Send command with payload (will be encrypted with stream cipher). */
	public synchronized void sendPacket(int command, ByteBuffer payload) throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + payload.remaining());
		
		/* Set IV. */
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
	public void sendPacket(int command) throws ProtocolException {
		this.sendPacket(command, ByteBuffer.allocate(0));
	}
	
	/* Receive a packet (will be decrypted with stream cipher). */
	public void receivePacket() throws ProtocolException {
		byte[] header = new byte[3];
		int command, payloadLength, headerLength = 3, macLength = 4;
		
		/* Read header. */
		if(this.receive(header, headerLength) != headerLength){
			throw new ProtocolException("Failed to read header.");
		}
		
		/* Set IV. */
		this.session.shannonRecv.nonce(IntegerUtilities.toBytes(this.session.keyRecvIv));
		
		/* Decrypt header. */
		this.session.shannonRecv.decrypt(header);
		
		/* Get command and payload length from header. */
		ByteBuffer headerBuffer = ByteBuffer.wrap(header);
		
		command       = headerBuffer.get()      & 0xff;
		payloadLength = headerBuffer.getShort() & 0xffff;
		
		/* Allocate buffer. Account for MAC. */
		byte[]     bytes  = new byte[payloadLength + macLength];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		/* Limit buffer to payload length, so we can read the payload. */
		buffer.limit(payloadLength);
		
		try{
			for(int n = payloadLength, r; n > 0 && (r = this.channel.read(buffer)) > 0; n -= r);
		}
		catch(IOException e){
			throw new ProtocolException("Failed to read payload!", e);
		}
		
		/* Extend it again to payload and mac length. */
		buffer.limit(payloadLength + macLength);
		
		try{
			for(int n = macLength, r; n > 0 && (r = this.channel.read(buffer)) > 0; n -= r);
		}
		catch(IOException e){
			throw new ProtocolException("Failed to read MAC!", e);
		}
		
		/* Decrypt payload. */
		this.session.shannonRecv.decrypt(bytes);
		
		/* Get payload bytes from buffer (throw away MAC). */
		byte[] payload = new byte[payloadLength];
		
		buffer.flip();
		buffer.get(payload);
		
		/* Increment IV. */
		this.session.keyRecvIv++;
		
		/* Fire events. */
		for(CommandListener listener : this.listeners){
			listener.commandReceived(command, payload);
		}
	}
	
	/* Send cache hash. */
	public void sendCacheHash() throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(20);
		
		buffer.put(this.session.cacheHash);
		buffer.flip();
		
		this.sendPacket(Command.COMMAND_CACHEHASH, buffer);
	}
	
	/* Request ads. The response is GZIP compressed XML. */
	public void sendAdRequest(ChannelListener listener, int type) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Ad-Channel", Channel.Type.TYPE_AD, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 1);
		
		/* Append channel id and ad type. */
		buffer.putShort((short)channel.getId());
		buffer.put((byte)type); /* 0: audio, 1: banner, 2: fullscreen-banner, 3: unknown.  */
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQUESTAD, buffer);
	}
	
	/* Get a toplist. The response comes as GZIP compressed XML. */
	public void sendToplistRequest(ChannelListener listener, Map<String, String> params) throws ProtocolException {
		/* Check if type parameter is present. */
		if(!params.containsKey("type")){
			throw new IllegalArgumentException("Parameter 'type' not given!");
		}
		
		/* Create a map of parameters and calculate their length. */
		Map<byte[], byte[]> parameters       = new HashMap<byte[], byte[]>();
		int                 parametersLength = 0;
		
		for(Entry<String, String> param : params.entrySet()){
			if(param.getKey() == null || param.getValue() == null){
				continue;
			}
			
			byte[] key   = param.getKey().getBytes(Charset.forName("UTF-8"));
			byte[] value = param.getValue().getBytes(Charset.forName("UTF-8"));
			
			parametersLength += 1 + 2 + key.length + value.length;
			
			parameters.put(key, value);
		}
		
		/* Create channel and buffer. */
		Channel    channel = new Channel("Toplist-Channel", Channel.Type.TYPE_TOPLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 2 + parametersLength);
		
		/* Append channel id, some values, query length and query. */
		buffer.putShort((short)channel.getId());
		buffer.putInt(0x00000000);
		
		for(Entry<byte[], byte[]> parameter : parameters.entrySet()){
			byte[] key   = parameter.getKey();
			byte[] value = parameter.getValue();
			
			buffer.put((byte)key.length);
			buffer.putShort((short)value.length);
			buffer.put(key);
			buffer.put(value);
		}
		
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_GETTOPLIST, buffer);
	}
	
	/* Request image using a 20 byte id. The response is a JPG. */
	public void sendImageRequest(ChannelListener listener, String id) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Image-Channel", Channel.Type.TYPE_IMAGE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 20);
		
		/* Check length of id. */
		if(id.length() != 40){
			throw new IllegalArgumentException("Image id needs to have a length of 40.");
		}
		
		/* Append channel id and image hash. */
		buffer.putShort((short)channel.getId());
		buffer.putShort((short)0x0000);
		buffer.put(Hex.toBytes(id));
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_IMAGE, buffer);
	}
	
	/* Search music. The response comes as GZIP compressed XML. */
	public void sendSearchQuery(ChannelListener listener, String query, int offset, int limit) throws ProtocolException {
		/* Create channel and buffer. */
		byte[]     queryBytes = query.getBytes(Charset.forName("UTF-8"));
		Channel    channel    = new Channel("Search-Channel", Channel.Type.TYPE_SEARCH, listener);
		ByteBuffer buffer     = ByteBuffer.allocate(2 + 2 + 6 * 4 + 2 + 1 + queryBytes.length);
		
		/* Check offset and limit. */
		if(offset < 0){
			throw new IllegalArgumentException("Offset needs to be >= 0");
		}
		else if((limit < 0 && limit != -1) || limit == 0){
			throw new IllegalArgumentException("Limit needs to be either -1 for no limit or > 0");
		}
		
		/* Append channel id, some unknown values, query length and query. */
		buffer.putShort((short)channel.getId());
		buffer.putShort((short)0x0000); /* Unknown. */
		buffer.putInt(offset); /* Result offset. */
		buffer.putInt(limit); /* Reply limit. */
		buffer.putInt(0x00000000); /* Unknown. */
		buffer.putInt(0xFFFFFFFF); /* Unknown. */
		buffer.putInt(0x00000000); /* Unknown. */
		buffer.putInt(0xFFFFFFFF); /* Unknown. */
		buffer.putShort((short)0x0000); /* Unknown. */
		buffer.put((byte)queryBytes.length);
		buffer.put(queryBytes);
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_SEARCH, buffer);
	}
	
	/* Search music. The response comes as GZIP compressed XML. */
	public void sendSearchQuery(ChannelListener listener, String query) throws ProtocolException {
		this.sendSearchQuery(listener, query, 0, -1);
	}
	
	/* Request AES key for a track. */
	public void sendAesKeyRequest(ChannelListener listener, Track track, File file) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("AES-Key-Channel", Channel.Type.TYPE_AESKEY, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(20 + 16 + 2 + 2 + 2);
		
		/* Request the AES key for this file by sending the file id and track id. */
		buffer.put(Hex.toBytes(file.getId())); /* 20 bytes */
		buffer.put(Hex.toBytes(track.getId())); /* 16 bytes */
		buffer.putShort((short)0x0000);
		buffer.putShort((short)channel.getId());
		buffer.putShort((short)0x0000);
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_REQKEY, buffer);
	}
	
	/* Notify server we're going to play. */
	public void sendPlayRequest() throws ProtocolException {
		/* 
		 * Notify the server about our intention to play music, there by allowing
		 * it to request other players on the same account to pause.
		 * 
		 * Yet another client side restriction to annony those who share their
		 * Spotify account with not yet invited friends. And as a bonus it won't
		 * play commercials and waste bandwidth in vain.
		 */
		this.sendPacket(Command.COMMAND_REQUESTPLAY);
	}
	
	/*
	 * Request a part of the encrypted file from the server.
	 * 
	 * The data should be decrypted using AES key in CTR mode
	 * with AES key provided and a static IV, incremented for
	 * each 16 byte data processed.
	 */
	public void sendSubstreamRequest(ChannelListener listener, Track track, File file, int offset, int length) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Substream-Channel", Channel.Type.TYPE_SUBSTREAM, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 2 + 2 + 2 + 2 + 2 + 4 + 20 + 4 + 4);
		
		/* Append channel id. */
		buffer.putShort((short)channel.getId());
		
		/* Unknown 10 bytes. */
		buffer.putShort((short)0x0800);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x0000);
		buffer.putShort((short)0x4e20);
		
		/* Unknown (static value) */
		buffer.putInt(200 * 1000);
		
		/* 20 bytes file id. */
		buffer.put(Hex.toBytes(file.getId()));
		
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
	
	/* TODO: untested. */
	public void sendChannelAbort(int id) throws ProtocolException {
		/* Create channel and buffer. */
		ByteBuffer buffer  = ByteBuffer.allocate(2);
		
		/* Append channel id. */
		buffer.putShort((short)id);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_CHANNELABRT, buffer);
	}
	
	/*
	 * Get metadata for an artist (type = 1), album (type = 2) or a
	 * list of tracks (type = 3). The response comes as compressed XML.
	 */
	public void sendBrowseRequest(ChannelListener listener, int type, Collection<String> ids) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Browse-Channel", Channel.Type.TYPE_BROWSE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 1 + ids.size() * 16 + ((type == 1 || type == 2)?4:0));
		
		/* Check arguments. */
		if(type != 1 && type != 2 && type != 3){
			throw new IllegalArgumentException("Type needs to be 1, 2 or 3.");
		}
		else if((type == 1 && type == 2) && ids.size() != 1){
			throw new IllegalArgumentException("Types 1 and 2 only accept a single id.");
		}
		
		/* Append channel id and type. */
		buffer.putShort((short)channel.getId());
		buffer.putShort((short)0x0000); /* Unknown. */
		buffer.put((byte)type);
		
		/* Append (16 byte binary, 32 byte hex string) ids. */
		for(String id : ids){
			/* Check length of id. */
			if(id.length() != 32){
				throw new IllegalArgumentException("Id needs to have a length of 32.");
			}
			
			buffer.put(Hex.toBytes(id));
		}
		
		/* Append zero. */
		if(type == 1 || type == 2){
			buffer.putInt(0); /* Timestamp of cached version? */
		}
		
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_BROWSE, buffer);
	}
	
	/* Browse with only one id. */
	public void sendBrowseRequest(ChannelListener listener, int type, String id) throws ProtocolException {
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(id);
		
		this.sendBrowseRequest(listener, type, list);
	}
	
	/* Request replacements for a list of tracks. The response comes as compressed XML. */
	public void sendReplacementRequest(ChannelListener listener, Collection<Track> tracks) throws ProtocolException {
		/* Calculate data length. */
		int dataLength = 0;
		
		for(Track track : tracks){
			if(track.getArtist() != null && track.getArtist().getName() != null){
				dataLength += track.getArtist().getName().getBytes().length;
			}
			
			if(track.getAlbum() != null && track.getAlbum().getName() != null){
				dataLength += track.getAlbum().getName().getBytes().length;
			}
			
			if(track.getTitle() != null){
				dataLength += track.getTitle().getBytes().length;
			}
			
			if(track.getLength() != -1){
				dataLength += Integer.toString(track.getLength() / 1000).getBytes().length;
			}
			
			dataLength += 4; /* Separators */
		}
		
		/* Create channel and buffer. */
		Channel    channel = new Channel("Browse-Channel", Channel.Type.TYPE_BROWSE, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 2 + 1 + dataLength);
		
		/* Append channel id and type. */
		buffer.putShort((short)channel.getId());
		buffer.putShort((short)0x0000); /* Unknown. */
		buffer.put((byte)0x06);
		
		/* Append track info. */
		for(Track track : tracks){
			if(track.getArtist() != null && track.getArtist().getName() != null){
				buffer.put(track.getArtist().getName().getBytes());
			}
			
			buffer.put((byte)0x01); /* Separator. */
			
			if(track.getAlbum() != null && track.getAlbum().getName() != null){
				buffer.put(track.getAlbum().getName().getBytes());
			}
			
			buffer.put((byte)0x01); /* Separator. */
			
			if(track.getTitle() != null){
				buffer.put(track.getTitle().getBytes());
			}
			
			buffer.put((byte)0x01); /* Separator. */
			
			if(track.getLength() != -1){
				buffer.put(Integer.toString(track.getLength() / 1000).getBytes());
			}
			
			buffer.put((byte)0x00); /* Separator. */
		}
		
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_BROWSE, buffer);
	}
	
	/* Request playlist details. The response comes as plain XML. */
	public void sendPlaylistRequest(ChannelListener listener, String id) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Playlist-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 16 + 1 + 4 + 4 + 4 + 1);
		
		/* Check length of id. */
		if(id != null && id.length() != 32){
			throw new IllegalArgumentException("Playlist id needs to have a length of 32.");
		}
		
		/* Append channel id, playlist id and some bytes... */
		buffer.putShort((short)channel.getId());
		
		/* Playlist container. */
		if(id == null){
			buffer.put(Hex.toBytes("00000000000000000000000000000000")); /* 16 bytes */
			buffer.put((byte)0x00); /* Playlist container identifier. */
		}
		/* Normal playlist. */
		else{
			buffer.put(Hex.toBytes(id)); /* 16 bytes */
			buffer.put((byte)0x02); /* Playlist identifier. */
		}
		/*
		 * TODO: Other playlist identifiers (e.g. 0x03, starred tracks? inbox?).
		 */
		
		/* TODO: Use those fields to request only the information needed. */
		buffer.putInt(-1); /* Revision. -1: no cached data. */
		buffer.putInt(0); /* Number of entries. */
		buffer.putInt(1); /* Checksum. */
		buffer.put((byte)0x00); /* Collaborative. */
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_GETPLAYLIST, buffer);
	}
	
	/* Change playlist container. The response comes as plain XML. */
	public void sendChangePlaylistContainer(ChannelListener listener, PlaylistContainer playlistContainer, String xml) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Change-Playlist-Container-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 16 + 1 + 4 + 4 + 4 + 1 + 1 + xml.getBytes().length);
		
		/* Append channel id, playlist id and some bytes... */
		buffer.putShort((short)channel.getId());
		buffer.put(Hex.toBytes("00000000000000000000000000000000")); /* 16 bytes */
		buffer.put((byte)0x00); /* Playlists identifier. */
		buffer.putInt((int)playlistContainer.getRevision());
		buffer.putInt(playlistContainer.getPlaylists().size());
		buffer.putInt((int)playlistContainer.getChecksum());
		buffer.put((byte)0x00); /* Collaborative */
		buffer.put((byte)0x03); /* Unknown */
		buffer.put(xml.getBytes());
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_CHANGEPLAYLIST, buffer);
	}
	
	/* Change playlist. The response comes as plain XML. */
	public void sendChangePlaylist(ChannelListener listener, Playlist playlist, String xml) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Change-Playlist-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 16 + 1 + 4 + 4 + 4 + 1 + 1 + xml.getBytes().length);
		
		/* Append channel id, playlist id and some bytes... */
		buffer.putShort((short)channel.getId());
		buffer.put(Hex.toBytes(playlist.getId())); /* 16 bytes */
		buffer.put((byte)0x02); /* Playlist identifier. */
		buffer.putInt((int)playlist.getRevision());
		buffer.putInt(playlist.getTracks().size());
		buffer.putInt((int)playlist.getChecksum());
		buffer.put((byte)(playlist.isCollaborative()?0x01:0x00));
		buffer.put((byte)0x03); /* Unknown */
		buffer.put(xml.getBytes());
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_CHANGEPLAYLIST, buffer);
	}
	
	/* Create playlist. The response comes as plain XML. */
	public void sendCreatePlaylist(ChannelListener listener, Playlist playlist, String xml) throws ProtocolException {
		/* Create channel and buffer. */
		Channel    channel = new Channel("Change-Playlist-Channel", Channel.Type.TYPE_PLAYLIST, listener);
		ByteBuffer buffer  = ByteBuffer.allocate(2 + 16 + 1 + 4 + 4 + 4 + 1 + 1 + xml.getBytes().length);
		
		/* Append channel id, playlist id and some bytes... */
		buffer.putShort((short)channel.getId());
		buffer.put(Hex.toBytes(playlist.getId())); /* 16 bytes */
		buffer.put((byte)0x02); /* Playlist identifier. */
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(-1); /* -1: Create playlist. */
		buffer.put((byte)(playlist.isCollaborative()?0x01:0x00));
		buffer.put((byte)0x03); /* Unknown */
		buffer.put(xml.getBytes());
		buffer.flip();
		
		/* Register channel. */
		Channel.register(channel);
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_CHANGEPLAYLIST, buffer);
	}
	
	/* Ping reply (pong). */
	public void sendPong() throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		/* TODO: Append timestamp? */
		buffer.putInt(0x00000000);
		buffer.flip();
		
		/* Send packet. */
		this.sendPacket(Command.COMMAND_PONG, buffer);
	}
	
	/* Send bytes. */
	private void send(ByteBuffer buffer) throws ProtocolException {
		try{
			this.channel.write(buffer);
		}
		catch (IOException e){
			throw new ProtocolException("Error writing data to socket!", e);
		}
	}
	
	/* Receive a single byte. */
	private int receive() throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		
		try{
			this.channel.read(buffer);
			
			buffer.flip();
			
			return buffer.get() & 0xff;
		}
		catch(IOException e){
			throw new ProtocolException("Error reading data from socket!", e);
		}
	}
	
	/* Receive bytes. */
	private int receive(byte[] buffer, int len) throws ProtocolException {
		return this.receive(buffer, 0, len);
	}
	
	/* Receive bytes. */
	private int receive(byte[] bytes, int off, int len) throws ProtocolException {
		ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);
		int n = 0;
		
		try{
			for(int r; n < len && (r = this.channel.read(buffer)) > 0; n += r);
		}
		catch(IOException e){
			throw new ProtocolException("Error reading data from socket!", e);
		}
		
		return n;
	}
}
