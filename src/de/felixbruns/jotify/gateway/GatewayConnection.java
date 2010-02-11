package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.HttpExchange;

import de.felixbruns.jotify.cache.*;
import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.exceptions.*;
import de.felixbruns.jotify.gateway.stream.ChannelStreamer;
import de.felixbruns.jotify.media.*;
import de.felixbruns.jotify.media.parser.*;
import de.felixbruns.jotify.player.*;
import de.felixbruns.jotify.protocol.*;
import de.felixbruns.jotify.protocol.channel.*;

public class GatewayConnection implements Runnable, CommandListener, Player {
	private Session      session;
	private Protocol     protocol;
	private User         user;
	private Semaphore    wait;
	private Cache        cache;
	private long         timeout;
	private TimeUnit     unit;
	private GatewayPlayer player;
	
	/**
	 * Enum for browsing media.
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
	 * Create a new GatewayConnection using the default {@link Cache}
	 * implementation and timeout value (10 seconds).
	 */
	public GatewayConnection(){
		this(new FileCache(), 10, TimeUnit.SECONDS);
	}
	
	/**
	 * Create a new GatewayConnection using a specified {@link Cache}
	 * implementation and timeout. Note: A {@link TimeoutException}
	 * may also be caused by geographical restrictions.
	 * 
	 * @param cache   Cache implementation to use.
	 * @param timeout Timeout value to use.
	 * @param unit    TimeUnit to use for timeout.
	 * 
	 * @see MemoryCache
	 * @see FileCache
	 */
	public GatewayConnection(Cache cache, long timeout, TimeUnit unit){
		this.session  = new Session();
		this.protocol = null;
		this.user     = null;
		this.wait     = new Semaphore(2);
		this.cache    = cache;
		this.timeout  = timeout;
		this.unit     = unit;
		this.player   = null;
		
		/* Acquire permits (country, prodinfo). */
		this.wait.acquireUninterruptibly(2);
	}
	
	/**
	 * Set timeout for requests.
	 * 
	 * @param timeout Timeout value to use.
	 * @param unit    TimeUnit to use for timeout.
	 */
	public void setTimeout(long timeout, TimeUnit unit){
		this.timeout = timeout;
		this.unit    = unit;
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
		/* Check if we're already logged in. */
		if(this.protocol != null){
			throw new IllegalStateException("Already logged in!");
		}
		
		/* Authenticate session and get protocol. */
		this.protocol = this.session.authenticate(username, password);
		
		/* Create user object. */
		this.user = new User(username);
		
		/* Create player. */
		this.player = new GatewayPlayer(this.protocol);
		
		/* Add command handler. */
		this.protocol.addListener(this);
	}
	
	/**
	 *  Closes the connection to a Spotify server.
	 *  
	 *  @throws ConnectionException
	 */
	public void close() throws ConnectionException {
		/* This will make receivePacket return immediately. */
		if(this.protocol != null){
			this.protocol.disconnect();
		}
		
		/* Reset protocol to 'null'. */
		this.protocol = null;
	}
	
	/**
	 *  Continuously receives packets in order to handle them.
	 *  Use a {@link Thread} to run this.
	 */
	public void run(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("You need to login first!");
		}
		
		/* Continuously receive packets until connection is closed. */
		try{
			while(true){
				this.protocol.receivePacket();
			}
		}
		catch(ProtocolException e){
			/* Connection was closed. */
		}
	}
	
	/**
	 * Get user info.
	 * 
	 * @return A xml string.
	 */
	public String user(){
		/* Wait for data to become available (country, prodinfo). */
		try{
			if(!this.wait.tryAcquire(2, this.timeout, this.unit)){
				throw new TimeoutException("Timeout while waiting for user data.");
			}
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		catch(TimeoutException e){
			throw new RuntimeException(e);
		}
		
		/* Release so this can be called again. */
		this.wait.release(2);
		
		/* Build xml string. */
		String xml =
			"<user>" + 
				"<name>" + this.user.getName() + "</name>" +
				"<country>" + this.user.getCountry() + "</country>" +
				"<type>" + this.user.getProperty("type") + "</type>" +
			"</user>";
		
		return xml;
	}
	
	/**
	 * Fetch a toplist.
	 * 
	 * @param type     A toplist type. e.g. "artist", "album" or "track".
	 * @param region   A region code or null. e.g. "SE" or "DE".
	 * @param username A username or null.
	 * 
	 * @return A xml string.
	 */
	public String toplist(String type, String region, String username) throws TimeoutException {
		/* Create channel callback and parameter map. */
		ChannelCallback callback   = new ChannelCallback();
		Map<String, String> params = new HashMap<String, String>();
		
		/* Add parameters. */
		params.put("type", type);
		params.put("region", region);
		params.put("username", username);
		
		/* Send search query. */
		try{
			this.protocol.sendToplistRequest(callback, params);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data. */
		byte[] data = callback.get(this.timeout, this.unit);
		
		/* Return xml string. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query Your search query.
	 * 
	 * @return A xml string.
	 */
	public String search(String query) throws TimeoutException {
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send search query. */
		try{
			this.protocol.sendSearchQuery(callback, query);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data. */
		byte[] data = callback.get(this.timeout, this.unit);
		
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
	public byte[] image(String id) throws TimeoutException {
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
			data = callback.get(this.timeout, this.unit);
			
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
	public String browse(BrowseType type, String id) throws TimeoutException {
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(callback, type.getValue(), id);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data. */
		byte[] data = callback.get(this.timeout, this.unit);
		
		/* Load XML. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Browse multiple tracks info.
	 * 
	 * @param ids Ids of tracks to browse.
	 * 
	 * @return A xml string.
	 */
	public String browse(Collection<String> ids) throws TimeoutException {
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(callback, BrowseType.TRACK.getValue(), ids);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data. */
		byte[] data = callback.get(this.timeout, this.unit);
		
		/* Load XML. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Get a list of stored playlists.
	 * 
	 * @return A xml string.
	 */
	public String playlistContainer() throws TimeoutException {
		/* Create channel callback. */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send stored playlists request. */
		try{
			this.protocol.sendPlaylistRequest(callback, null);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = callback.get(this.timeout, this.unit);
		
		/* Return string. */
		return	"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
				new String(data, Charset.forName("UTF-8")) +
				"</playlist>";
	}
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A xml string.
	 */
	public String playlist(String id) throws TimeoutException {
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
		byte[] data = callback.get(this.timeout, this.unit);
		
		/* Return string. */
		return	"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
				new String(data, Charset.forName("UTF-8")) +
				"</playlist>";
	}
	
	/**
	 * Stream a track to an output stream.
	 * 
	 * @throws IOException
	 */
	public void stream(String id, String fileId, HttpExchange exchange) throws IOException, TimeoutException  {
		/* Browse track. */
		Track track = new Track(id);
		
		track.addFile(new File(fileId, ""));
		
		/* Create channel callbacks. */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send play request (token notify + AES key). */
		try{
			this.protocol.sendAesKeyRequest(callback, track);
		}
		catch(ProtocolException e){
			exchange.sendResponseHeaders(404, -1);
			
			return;
		}
		
		/* Get AES key. */
		byte[] key = callback.get(this.timeout, this.unit);
		
		/* Stream channel. */
		new ChannelStreamer(this.protocol, track, key, exchange);
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
				this.user.setCountry(new String(payload, Charset.forName("UTF-8")));
				
				/* Release 'country' permit. */
				this.wait.release();
				
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip 11 byte header... */
				this.user.setNotification(new String(
					Arrays.copyOfRange(payload, 11, payload.length), Charset.forName("UTF-8")
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				this.user = XMLUserParser.parseUser(payload, "UTF-8", this.user);
				
				/* Release 'prodinfo' permit. */
				this.wait.release();
				
				/* Payload is uncompressed XML. */
				if(!this.user.isPremium()){
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
	
	public int length(){
		return this.player.length();
	}
	
	public void pause() {
		this.player.pause();
	}
	
	public void play(Track track, PlaybackListener listener){
		this.player.play(track, listener);
	}
	
	public void play(){
		this.player.play();
	}
	
	public int position(){
		return this.player.position();
	}
	
	public void stop(){
		this.player.stop();
	}
	
	public float volume(){
		return this.player.volume();
	}
	
	public void volume(float volume){
		this.player.volume(volume);
	}
}
