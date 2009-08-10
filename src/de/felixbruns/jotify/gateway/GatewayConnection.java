package de.felixbruns.jotify.gateway;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.JotifyPlayer;
import de.felixbruns.jotify.cache.Cache;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.cache.MemoryCache;
import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.gateway.stream.ChannelStreamer;
import de.felixbruns.jotify.gateway.stream.HTTPStreamer;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.User;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;
import de.felixbruns.jotify.protocol.Command;
import de.felixbruns.jotify.protocol.CommandListener;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.Session;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.protocol.channel.ChannelHeaderCallback;
import de.felixbruns.jotify.util.GZIP;
import de.felixbruns.jotify.util.XML;
import de.felixbruns.jotify.util.XMLElement;

public class GatewayConnection implements Runnable, CommandListener, Player {
	private Session      session;
	private Protocol     protocol;
	private User         user;
	private Semaphore    wait;
	private Cache        cache;
	private long         timeout;
	private TimeUnit     unit;
	private JotifyPlayer player;
	
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
		this.player = new JotifyPlayer(this.protocol);
		
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
				"<type>" + this.user.getType() + "</type>" +
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
	public String toplist(String type, String region, String username){
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
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.get(this.timeout, this.unit));
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
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
		byte[] data = GZIP.inflate(callback.get(this.timeout, this.unit));
		
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
		byte[] data = GZIP.inflate(callback.get(this.timeout, this.unit));
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
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
	public String browse(Collection<String> ids){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(callback, BrowseType.TRACK.getValue(), ids);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.get(this.timeout, this.unit));
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Get a list of stored playlists.
	 * 
	 * @return A xml string.
	 */
	public String playlists(){
		/* Create channel callback. */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send stored playlists request. */
		try{
			this.protocol.sendUserPlaylistsRequest(callback);
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
		byte[] data = callback.get(this.timeout, this.unit);
		
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
		Result result = Result.fromXMLElement(XML.load(browse(BrowseType.TRACK, id)));
		Track  track  = result.getTracks().get(0);
		
		/* Create channel callbacks. */
		ChannelCallback       callback       = new ChannelCallback();
		ChannelHeaderCallback headerCallback = new ChannelHeaderCallback();
		
		/* Send play request (token notify + AES key). */
		try{
			this.protocol.sendAesKeyRequest(callback, track);
		}
		catch(ProtocolException e){
			return;
		}
		
		/* Get AES key. */
		byte[] key = callback.get(this.timeout, this.unit);
		
		/* Send header request to check for HTTP stream. */
		try{
			this.protocol.sendSubstreamRequest(headerCallback, track, 0, 0);
		}
		catch(ProtocolException e){
			return;
		}
		
		/* Get list of HTTP stream URLs. */
		List<String> urls = headerCallback.get(this.timeout, this.unit);
		
		/* If we got 4 HTTP stream URLs use them, otherwise use default channel streaming. */
		if(urls.size() == 4){
			new HTTPStreamer(urls, track, key, stream);
		}
		else{
			new ChannelStreamer(this.protocol, track, key, stream);
		}
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
				XMLElement prodinfoElement = XML.load(new String(payload, Charset.forName("UTF-8")));
				
				this.user = User.fromXMLElement(prodinfoElement, this.user);
				
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
