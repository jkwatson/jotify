package de.felixbruns.jotify;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import de.felixbruns.jotify.cache.Cache;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.cache.MemoryCache;
import de.felixbruns.jotify.crypto.Hash;
import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.ChannelPlayer;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.protocol.Command;
import de.felixbruns.jotify.protocol.CommandListener;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.Session;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.util.GZIP;
import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.XML;
import de.felixbruns.jotify.util.XMLElement;

public class Jotify implements Runnable, CommandListener {
	private Session       session;
	private Protocol      protocol;
	private ChannelPlayer player;
	private Cache         cache;
	private boolean       close;
	private float         volume;
	
	/**
	 * Constants for browsing media.
	 */
	public static final int BROWSE_ARTIST = 1;
	public static final int BROWSE_ALBUM  = 2;
	public static final int BROWSE_TRACK  = 3;
	
	/**
	 * Create a new Jotify instance using the default client revision
	 * and {@link Cache} implementation ({@link FileCache}).
	 */
	public Jotify(){
		this(-1);
	}
	
	/**
	 * Create a new Jotify instance using a specified client revision
	 * and a {@link FileCache} implementation.
	 * 
	 * @param revision Revision number to use when connecting.
	 * 
	 * @see FileCache
	 */
	public Jotify(int revision){
		this(revision, new FileCache());
	}
	
	/**
	 * Create a new Jotify instance using a specified client revision
	 * and {@link Cache} implementation.
	 * 
	 * @param revision Revision number to use when connecting.
	 * @param cache    Cache implementation to use.
	 * 
	 * @see MemoryCache
	 * @see FileCache
	 */
	public Jotify(int revision, Cache cache){
		this.session  = new Session(revision);
		this.protocol = null;
		this.player   = null;
		this.cache    = cache;
		this.close    = false;
		this.volume   = 1.0f;
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
	 * @return A {@link Result} object.
	 * 
	 * @see Result
	 */
	public Result search(String query){
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
		byte[] data = GZIP.inflate(callback.getData());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		XMLElement resultElement = XML.load(data, Charset.forName("UTF-8"));
		
		/* Create result from XML. */
		return Result.fromXMLElement(query, resultElement);
	}
	
	/**
	 * Get an image (e.g. artist portrait or cover) by requesting
	 * it from the server or loading it from the local cache, if
	 * available.
	 * 
	 * @param id Id of the image to get.
	 * 
	 * @return An {@link Image} or null if the request failed.
	 * 
	 * @see Image
	 */
	public Image image(String id){
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
			data = callback.getData();
			
			/* Save to cache. */
			if(this.cache != null){
				this.cache.store("image", id, data);
			}
		}
		
		/* Create Image. */
		try{
			return ImageIO.read(new ByteArrayInputStream(data));
		}
		catch(IOException e){
			return null;
		}
	}
	
	/**
	 * Browse artist, album or track info.
	 * 
	 * @param type Type of media to browse for.
	 * @param id   Id of media to browse.
	 * 
	 * @return An {@link XMLElement} object holding the data or null
	 *         on failure.
	 * 
	 * @see BROWSE_ARTIST
	 * @see BROWSE_ALBUM
	 * @see BROWSE_TRACK
	 */
	private XMLElement browse(int type, String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(callback, type, id);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.getData());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		return XML.load(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Browse artist info.
	 * 
	 * @param artist An {@link Artist} object identifying the artist to browse.
	 * 
	 * @retrun A new {@link Artist} object holding more information about
	 *         the artist or null on failure.
	 * 
	 * @see Artist
	 */
	public Artist browse(Artist artist){
		/* Browse. */
		XMLElement artistElement = this.browse(BROWSE_ARTIST, artist.getId());
		
		if(artistElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Artist.fromXMLElement(artistElement);
	}
	
	/**
	 * Browse album info.
	 * 
	 * @param album An {@link Album} object identifying the album to browse.
	 * 
	 * @retrun A new {@link Album} object holding more information about
	 *         the album or null on failure.
	 * 
	 * @see Album
	 */
	public Album browse(Album album){
		/* Browse. */
		XMLElement albumElement = this.browse(BROWSE_ALBUM, album.getId());
		
		if(albumElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Album.fromXMLElement(albumElement);
	}
	
	/**
	 * Browse track info.
	 * 
	 * @param album A {@link Track} object identifying the track to browse.
	 * 
	 * @retrun A {@link Result} object holding more information about
	 *         the track or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browse(Track track){
		/* Browse. */
		XMLElement resultElement = this.browse(BROWSE_TRACK, track.getId());
		
		if(resultElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
	}
	
	/**
	 * Browse information for multiple tracks.
	 * 
	 * @param tracks A {@link List} of {@link Track} objects identifying
	 *               the tracks to browse.
	 * 
	 * @retrun A {@link Result} object holding more information about
	 *         the tracks or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browse(List<Track> tracks){
		/* Data buffer. */
		byte[] data;
		
		/* Create cache hash. */
		String hash = "";
		
		for(Track track : tracks){
			hash += track.getId();
		}
		
		hash = Hex.toHex(Hash.sha1(Hex.toBytes(hash)));
		
		/* Check cache. */
		if(this.cache != null && this.cache.contains("browse", hash)){
			data = this.cache.load("browse", hash);
		}
		else{
			/* Create channel callback */
			ChannelCallback callback = new ChannelCallback();
				
			/* Create id list. */
			List<String> ids = new ArrayList<String>();
				
			for(Track track : tracks){
				ids.add(track.getId());
			}
				
			/* Send browse request. */
			try{
				this.protocol.sendBrowseRequest(callback, BROWSE_TRACK, ids);
			}
			catch(ProtocolException e){
				return null;
			}
			
			/* Get data and inflate it. */
			data = GZIP.inflate(callback.getData());
			
			/* Cut off that last 0xFF byte... */
			data = Arrays.copyOfRange(data, 0, data.length - 1);
			
			/* Save to cache. */
			if(this.cache != null){
				this.cache.store("browse", hash, data);
			}
		}
		
		/* Load XML. */
		XMLElement resultElement = XML.load(data, Charset.forName("UTF-8"));
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
	}
	
	/**
	 * Get a list of stored playlists.
	 * 
	 * @return A {@link List} of {@link Playlist} objects or null on failure.
	 *         (Note: {@link Playlist} objects only hold id and author)
	 * 
	 * @see Playlist
	 */
	public PlaylistContainer playlists(){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		try{
			this.protocol.sendPlaylistRequest(callback, "0000000000000000000000000000000000");
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get data and inflate it. */
		byte[] data = callback.getData();
		
		/* Load XML. */
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		/* Create an return list. */
		return PlaylistContainer.fromXMLElement(playlistElement);
	}
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlist(String id, boolean useCache){
		/* Data buffer. */
		byte[] data;
		
		if(useCache && this.cache != null && this.cache.contains("playlist", id)){
			data = this.cache.load("playlist", id);
		}
		else{
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
			data = callback.getData();
			
			/* Save data to cache. */
			if(this.cache != null){
				this.cache.store("playlist", id, data);
			}
		}
		
		/* Load XML. */
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		/* Create and return playlist. */
		return Playlist.fromXMLElement(playlistElement, id);
	}
	
	public Playlist playlist(String id){
		return this.playlist(id, false);
	}
	
	public boolean playlistRename(Playlist playlist, String name){
		String user = this.session.getUsername();
		
		if(!playlist.getAuthor().equals(user)){
			return false;
		}
		
		String xml = String.format(
			"<change><ops><name>%s</name></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,%d</version>",
			name, new Date().getTime() / 1000, user,
			playlist.getRevision() + 1, playlist.getTracks().size(),
			playlist.getChecksum(), playlist.isCollaborative()?1:0
		);
		
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send change playlist request. */
		try{
			this.protocol.sendChangePlaylist(callback, playlist, xml);
		}
		catch(ProtocolException e){
			return false;
		}
		
		/* Get response. */
		byte[] data = callback.getData();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);		
		
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setChecksum(Long.parseLong(parts[2]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			return true;
		}
		
		return false;
	}
	
	public boolean playlistSetCollaborative(Playlist playlist, boolean collaborative){
		String user = this.session.getUsername();
		
		if(!playlist.getAuthor().equals(user)){
			return false;
		}
		
		String xml = String.format(
			"<change><ops><pub>%d</pub></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,%d</version>",
			collaborative?1:0, new Date().getTime() / 1000, user,
			playlist.getRevision() + 1, playlist.getTracks().size(),
			playlist.getChecksum(), playlist.isCollaborative()?1:0
		);
		
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send change playlist request. */
		try{
			this.protocol.sendChangePlaylist(callback, playlist, xml);
		}
		catch(ProtocolException e){
			return false;
		}
		
		/* Get response. */
		byte[] data = callback.getData();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);		
		
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setChecksum(Long.parseLong(parts[2]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Play a track in a background thread.
	 * 
	 * @param track A {@link Track} object identifying the track to be played.
	 */
	public void play(Track track, PlaybackListener listener){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send play request (token notify + AES key). */
		try{
			this.protocol.sendPlayRequest(callback, track);
		}
		catch(ProtocolException e){
			return;
		}
		
		/* Get AES key. */
		byte[] key = callback.getData();
		
		/* Create channel player. */
		this.player = new ChannelPlayer(this.protocol, track, key, listener);
		this.player.volume(this.volume);
		
		/* Start playing. */
		this.play();
	}
	
	/**
	 * Start playing or resume current track.
	 */
	public void play(){
		if(this.player != null){
			this.player.play();
		}
	}
	
	/**
	 * Pause playback of current track.
	 */
	public void pause(){
		if(this.player != null){
			this.player.stop();
		}
	}
	
	/**
	 * Stop playback of current track.
	 */
	public void stop(){
		if(this.player != null){
			this.player.close();
			
			this.player = null;
		}
	}
	
	/**
	 * Get length of current track.
	 * 
	 * @return Length in seconds or -1 if not available.
	 */
	public int length(){
		if(this.player != null){
			return this.player.length();
		}
		
		return -1;
	}
	
	/**
	 * Get playback position of current track.
	 * 
	 * @return Playback position in seconds or -1 if not available.
	 */
	public int position(){
		if(this.player != null){
			return this.player.position();
		}
		
		return -1;
	}
	
	/**
	 * Get volume.
	 * 
	 * @return A value from 0.0 to 1.0.
	 */
	public float volume(){
		if(this.player != null){
			return this.player.volume();
		}
		
		return -1;
	}
	
	/**
	 * Set volume.
	 * 
	 * @param volume A value from 0.0 to 1.0.
	 */
	public void volume(float volume){
		this.volume = volume;
		
		if(this.player != null){
			this.player.volume(this.volume);
		}
	}
	
	/**
	 * Handles incoming commands from the server.
	 * 
	 * @param command A command.
	 * @param payload Payload of packet.
	 */
	public void commandReceived(int command, byte[] payload){
		//System.out.format("< Command: 0x%02x Length: %d\n", command, payload.length);
		
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
			case Command.COMMAND_SHAHASH: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_COUNTRYCODE: {
				System.out.println("Country: " + new String(payload, Charset.forName("UTF-8")));
				
				break;
			}
			case Command.COMMAND_P2P_INITBLK: {
				/* Do nothing. */
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
					
					System.exit(0);
				}
				
				break;
			}
			case Command.COMMAND_WELCOME: {
				/* Request ads. */
				//this.protocol.sendAdRequest(new ChannelAdapter(), 0);
				//this.protocol.sendAdRequest(new ChannelAdapter(), 1);
				
				break;
			}
			case Command.COMMAND_PAUSE: {
				/* TODO: Show notification and pause. */
				
				break;
			}
		}
	}
	
	/**
	 * Main method for testing purposes.
	 * 
	 * @param args Commandline arguments.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		/* Create a spotify object. */
		Jotify jotify = new Jotify();
		
		jotify.login("username", "password");
		
		/* Start packet IO in the background. */
		new Thread(jotify).start();
		
		/* Get a list of this users playlists. */
		PlaylistContainer playlists = jotify.playlists();
		
		/* Load the first playlist. */
		Playlist playlist = jotify.playlist(playlists.getPlaylists().get(0).getId());
		
		/* Browse for playlist tracks. */
		jotify.browse(playlist.getTracks());
		
		/* Search for an artist / album / track. */
		Result result = jotify.search("Razorlight");
		
		/* Play first track in result (in the background). */
		jotify.play(result.getTracks().get(0), null);
		
		/* Browse the artist. */
		Artist artist = jotify.browse(result.getArtists().get(0));
		
		/* Load an image. */
		jotify.image(artist.getPortrait());
	}
}
