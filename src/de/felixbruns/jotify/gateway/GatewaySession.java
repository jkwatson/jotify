package de.felixbruns.jotify.gateway;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import de.felixbruns.jotify.cache.Cache;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.Command;
import de.felixbruns.jotify.protocol.CommandListener;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.Session;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.util.GZIP;

public class GatewaySession implements Runnable, CommandListener {
	private Session  session;
	private Protocol protocol;
	private Cache    cache;
	private boolean  close;
	
	/**
	 * Constants for browsing media.
	 */
	public enum BrowseType {
		ARTIST(1), ALBUM(2), TRACK(3);
		
		private int value;
		
		private BrowseType(int value){
			this.value = value;
		}
		
		public int getValue(){
			return this.value;
		}
		
		public static BrowseType valueOf(int value){
			for(BrowseType type : BrowseType.values()){
				if(type.value == value){
					return type;
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Create a new JotifySession instance.
	 */
	public GatewaySession(){
		this.session  = new Session();
		this.protocol = null;
		this.cache    = new FileCache();
		this.close    = false;
	}
	
	/**
	 * Login to Spotify using the specified username and password.
	 * 
	 * @param username Username to use.
	 * @param password Corresponding password.
	 * 
	 * @throws ConnectionException
	 * @throws AuthenticationException
	 */
	public void login(String username, String password) throws ConnectionException, AuthenticationException {
		/* Authenticate session. */
		this.protocol = this.session.authenticate(username, password);
		
		/* Add command handler. */
		this.protocol.addListener(this);
	}
	
	/**
	 *  Closes connection to a Spotify server.
	 *  
	 *  @throws ConnectionException
	 */
	public void close() throws ConnectionException {
		this.close = true;
		
		/* This will make receivePacket return immediately. */
		if(this.protocol != null){
			this.protocol.disconnect();
		}
	}
	
	/**
	 *  Continuously receives packets in order to handle them.
	 *  Use a {@link Thread} to run this.
	 */
	public void run(){
		if(this.protocol == null){
			throw new Error("You need to login first!");
		}
		
		try{
			while(!this.close){
				this.protocol.receivePacket();
			}
		}
		catch(ProtocolException e){
			/* Do nothing. Just disconnect. */
		}
		
		/* Disconnect. */
		try{
			this.protocol.disconnect();
		}
		catch(ConnectionException e){
			/* Don't care. */
		}
	}
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query Your search query.
	 * 
	 * @return A xml string.
	 */
	public String search(String query){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send search query. */
		try{
			this.protocol.sendSearchQuery(callback, query);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.get());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Return xml string. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Get an image (e.g. artist portrait or cover) by requesting
	 * it from the server.
	 * 
	 * @param id Id of the image to get.
	 * 
	 * @return An array of bytes.
	 */
	public byte[] image(String id){
		/* Data buffer. */
		byte[] data;
		
		/* Check cache. */
		if(this.cache != null && this.cache.contains("image", id)){
			data = this.cache.load("image", id);
		}
		else{
			/* Create channel callback */
			ChannelCallback callback = new ChannelCallback();
			
			/* Send image request. */
			try{
				this.protocol.sendImageRequest(callback, id);
			}
			catch(ProtocolException e){
				return null;
			}
			
			/* Get data. */
			data = callback.get();
			
			/* Save to cache. */
			if(this.cache != null){
				this.cache.store("image", id, data);
			}
		}
		
		/* Return image data. */
		return data;
	}
	
	/**
	 * Browse artist, album or track info.
	 * 
	 * @param type Type of media to browse for.
	 * @param id   Id of media to browse.
	 * 
	 * @return A xml string.
	 * 
	 * @see BrowseType
	 */
	public String browse(BrowseType type, String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(callback, type.getValue(), id);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.get());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A xml string.
	 */
	public String playlist(String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendPlaylistRequest(callback, id);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = callback.get();
		
		/* Return string. */
		return	"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
				new String(data, Charset.forName("UTF-8")) +
				"</playlist>";
	}
	
	/**
	 * Stream a track to an output stream.
	 */
	public void stream(String id, String fileId, OutputStream stream){
		/* Create track and set file id. */
		Track track = new Track(id, null, null, null);
		
		track.addFile(fileId);
		
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send play request (token notify + AES key). */
		try{
			this.protocol.sendAesKeyRequest(callback, track);
		}
		catch(ProtocolException e){
			return;
		}
		
		/* Get AES key. */
		byte[] key = callback.get();
		
		/* Create channel streamer. */
		new ChannelStreamer(this.protocol, track, key, stream);
	}
	
	/**
	 * Handles incoming commands from the server.
	 * 
	 * @param command A command.
	 * @param payload Payload of packet.
	 */
	public void commandReceived(int command, byte[] payload){
		switch(command){
			case Command.COMMAND_SECRETBLK: {
				/* Check length. */
				if(payload.length != 336){
					System.err.format("Got command 0x02 with len %d, expected 336!\n", payload.length);
				}
				
				/* Check RSA public key. */
				byte[] rsaPublicKey = RSA.keyToBytes(this.session.getRSAPublicKey());
				
				for(int i = 0; i < 128; i++){
					if(payload[16 + i] != rsaPublicKey[i]){
						System.err.format("RSA public key doesn't match! %d\n", i);
						
						break;
					}
				}
				
				/* Send cache hash. */
				try{
					this.protocol.sendCacheHash();
				}
				catch(ProtocolException e){
					/* Just don't care. */
				}
				
				break;
			}
			case Command.COMMAND_PING: {
				/* Ignore the timestamp but respond to the request. */
				/* int timestamp = IntegerUtilities.bytesToInteger(payload); */
				try{
					this.protocol.sendPong();
				}
				catch(ProtocolException e){
					/* Just don't care. */
				}
				
				break;
			}
			case Command.COMMAND_CHANNELDATA: {
				Channel.process(payload);
				
				break;
			}
			case Command.COMMAND_CHANNELERR: {
				Channel.error(payload);
				
				break;
			}
			case Command.COMMAND_AESKEY: {
				/* Channel id is at offset 2. AES Key is at offset 4. */
				Channel.process(Arrays.copyOfRange(payload, 2, payload.length));
				
				break;
			}
			case Command.COMMAND_COUNTRYCODE: {
				System.out.println("Country: " + new String(payload, Charset.forName("UTF-8")));
				
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip 11 byte header... */
				System.out.println("Notification: " + new String(
					Arrays.copyOfRange(payload, 11, payload.length), Charset.forName("UTF-8")
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				/* Payload is uncompressed XML. */
				if(!new String(payload, Charset.forName("UTF-8")).contains("<type>premium</type>")){
					System.err.println(
						"Sorry, you need a premium account to use jotify (this is a restriction by Spotify)."
					);
				}
				
				break;
			}
			default: {
				break;
			}
		}
	}
}
