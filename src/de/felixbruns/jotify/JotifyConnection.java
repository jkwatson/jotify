package de.felixbruns.jotify;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import de.felixbruns.jotify.cache.*;
import de.felixbruns.jotify.crypto.*;
import de.felixbruns.jotify.exceptions.*;
import de.felixbruns.jotify.media.*;
import de.felixbruns.jotify.player.*;
import de.felixbruns.jotify.protocol.*;
import de.felixbruns.jotify.protocol.channel.*;
import de.felixbruns.jotify.util.*;

public class JotifyConnection implements Jotify, CommandListener {
	private Session       session;
	private Protocol      protocol;
	private User          user;
	private Semaphore     userSemaphore;
	private ChannelPlayer player;
	private Cache         cache;
	private boolean       close;
	private float         volume;
	
	/**
	 * Constants for browsing media.
	 */
	private enum BrowseType {
		ARTIST(1), ALBUM(2), TRACK(3);
		
		private int value;
		
		private BrowseType(int value){
			this.value = value;
		}
		
		public int getValue(){
			return this.value;
		}
	}
	
	/**
	 * Create a new Jotify instance using the default client revision
	 * and {@link Cache} implementation ({@link FileCache}).
	 */
	public JotifyConnection(){
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
	public JotifyConnection(int revision){
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
	public JotifyConnection(int revision, Cache cache){
		this.session       = new Session(revision);
		this.protocol      = null;
		this.user          = null;
		this.userSemaphore = new Semaphore(2);
		this.player        = null;
		this.cache         = cache;
		this.close         = false;
		this.volume        = 1.0f;
		
		/* Acquire permits (country, prodinfo). */
		this.userSemaphore.acquireUninterruptibly(2);
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
		
		/* Create user object. */
		this.user = new User(username);
		
		/* Add command handler. */
		this.protocol.addListener(this);
	}
	
	/**
	 *  Closes the connection to a Spotify server.
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
			throw new IllegalStateException("You need to login first!");
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
	 * Get user info.
	 * 
	 * @return A {@link User} object.
	 * 
	 * @see User
	 */
	public User user(){
		/* Acquire permits (country, prodinfo). */
		this.userSemaphore.acquireUninterruptibly(2);
		
		/* Release so this can be called again. */
		this.userSemaphore.release(2);
		
		return this.user;
	}
	
	/**
	 * Fetch a toplist.
	 * 
	 * @param type     A toplist type. e.g. "artist", "album" or "track".
	 * @param region   A region code or null. e.g. "SE" or "DE".
	 * @param username A username or null.
	 * 
	 * @return A {@link Result} object.
	 * 
	 * @see Result
	 */
	public Result toplist(String type, String region, String username){
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
		byte[] data = GZIP.inflate(callback.get());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		XMLElement toplistElement = XML.load(data, Charset.forName("UTF-8"));
		
		/* Create result from XML. */
		return Result.fromXMLElement(toplistElement);
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
		/* Create channel callback. */
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
			/* Create channel callback. */
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
	 * @see BrowseType
	 */
	private XMLElement browse(BrowseType type, String id){
		/* Create channel callback. */
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
		return XML.load(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Browse artist info by id.
	 * 
	 * @param id An id identifying the artist to browse.
	 * 
	 * @retrun An {@link Artist} object holding more information about
	 *         the artist or null on failure.
	 * 
	 * @see Artist
	 */
	public Artist browseArtist(String id){
		/* Browse. */
		XMLElement artistElement = this.browse(BrowseType.ARTIST, id);
		
		if(artistElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Artist.fromXMLElement(artistElement);
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
		return this.browseArtist(artist.getId());
	}
	
	/**
	 * Browse album info by id.
	 * 
	 * @param id An id identifying the album to browse.
	 * 
	 * @retrun An {@link Album} object holding more information about
	 *         the album or null on failure.
	 * 
	 * @see Album
	 */
	public Album browseAlbum(String id){
		/* Browse. */
		XMLElement albumElement = this.browse(BrowseType.ALBUM, id);
		
		if(albumElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Album.fromXMLElement(albumElement);
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
		return this.browseAlbum(album.getId());
	}
	
	/**
	 * Browse track info by id.
	 * 
	 * @param id An id identifying the track to browse.
	 * 
	 * @retrun A {@link Result} object holding more information about
	 *         the track or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browseTrack(String id){
		/* Browse. */
		XMLElement resultElement = this.browse(BrowseType.TRACK, id);
		
		if(resultElement == null){
			return null;
		}
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
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
		return this.browseTrack(track.getId());
	}
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param tracks A {@link List} of ids identifying the tracks to browse.
	 * 
	 * @retrun A {@link Result} object holding more information about
	 *         the tracks or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browseTracks(List<String> ids){
		/* Data buffer. */
		byte[] data;
		
		/* Create cache hash. */
		String hash = "";
		
		for(String id : ids){
			hash += id;
		}
		
		hash = Hex.toHex(Hash.sha1(Hex.toBytes(hash)));
		
		/* Check cache. */
		if(this.cache != null && this.cache.contains("browse", hash)){
			data = this.cache.load("browse", hash);
		}
		else{
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
			data = GZIP.inflate(callback.get());
			
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
		/* Create id list. */
		List<String> ids = new ArrayList<String>();
			
		for(Track track : tracks){
			ids.add(track.getId());
		}
		
		return this.browseTracks(ids);
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
		/* Create channel callback. */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send stored playlists request. */
		try{
			this.protocol.sendUserPlaylistsRequest(callback);
		}
		catch(ProtocolException e){
			return PlaylistContainer.EMPTY;
		}
		
		/* Get data and inflate it. */
		try{
			byte[] data = callback.get(10, TimeUnit.SECONDS);
			
			/* Load XML. */
			XMLElement playlistElement = XML.load(
				"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
				new String(data, Charset.forName("UTF-8")) +
				"</playlist>"
			);
			
			/* Create an return list. */
			return PlaylistContainer.fromXMLElement(playlistElement);
		}
		catch(Exception e){
			return PlaylistContainer.EMPTY;
		}
	}
	
	/**
	 * Add a playlist to the list of stored playlists.
	 * 
	 * @param playlists A {@link PlaylistContainer} to add the playlist to.
	 * @param playlist  The {@link Playlist} to be added.
	 * @param position  The target position of the playlist.
	 * 
	 * @return true on success and false on failure.
	 * 
	 * @see PlaylistContainer
	 */
	public boolean playlistsAddPlaylist(PlaylistContainer playlists, Playlist playlist, int position){
		String user = this.session.getUsername();
		
		position = playlists.getPlaylists().size() - 1;
		
		/* First add the playlist to calculate the new checksum. */
		playlists.getPlaylists().add(position, playlist);
		
		String xml = String.format(
			"<change><ops><add><i>%d</i><items>%s02</items></add></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,0</version>",
			position, playlist.getId(), new Date().getTime() / 1000, user,
			playlists.getRevision() + 1, playlists.getPlaylists().size(),
			playlists.getChecksum()
		);
		
		/* Remove the playlist again, because we need the old checksum for sending. */
		playlists.getPlaylists().remove(position);
		
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send change playlists request. */
		try{
			this.protocol.sendChangeUserPlaylists(callback, playlists, xml);
		}
		catch(ProtocolException e){
			return false;
		}
		
		/* Get response. */
		byte[] data = callback.get();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);		
		
		/* Check for success. */
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlists.setRevision(Long.parseLong(parts[0]));
			
			/* Add the track, since operation was successful. */
			playlists.getPlaylists().add(position, playlist);
			
			if(playlists.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	// TODO: playlistsAddPlaylists, playlistsRemovePlaylist(s), playlistsMovePlaylist(s)
	
	/**
	 * Get a playlist.
	 * 
	 * @param id       Id of the playlist to load.
	 * @param useCache Whether to use a cached version if available or not.
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
			
			/* Send playlist request. */
			try{
				this.protocol.sendPlaylistRequest(callback, id);
			}
			catch(ProtocolException e){
				return null;
			}
			
			/* Get data and inflate it. */
			data = callback.get(10, TimeUnit.SECONDS);
			
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
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlist(String id){
		return this.playlist(id, false);
	}
	
	/**
	 * Create a playlist.
	 * 
	 * @param name          The name of the playlist to create.
	 * @param collaborative If the playlist shall be collaborative.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlistCreate(String name, boolean collaborative){
		String   id       = Hex.toHex(RandomBytes.randomBytes(16));
		String   user     = this.session.getUsername();
		Playlist playlist = new Playlist(id, name, user, collaborative);
		
		String xml = String.format(
			"<id-is-unique/><change><ops><create/><name>%s</name></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>0000000001,0000000000,0000000001,%d</version>",
			playlist.getName(), new Date().getTime() / 1000,
			playlist.getAuthor(), playlist.isCollaborative()?1:0
		);
		
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send change playlist request. */
		try{
			this.protocol.sendCreatePlaylist(callback, playlist, xml);
		}
		catch(ProtocolException e){
			return null;
		}
		
		/* Get response. */
		byte[] data = callback.get();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);		
		
		/* Check for success. */
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return playlist;
		}
		
		return null;
	}
	
	/**
	 * Add a track to a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param track    The track to be added.
	 * @param position The target position of the added track.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistAddTrack(Playlist playlist, Track track, int position){
		String user = this.session.getUsername();
		
		if(!playlist.isCollaborative() && !playlist.getAuthor().equals(user)){
			return false;
		}
		
		/* First add the track to calculate the new checksum. */
		playlist.getTracks().add(position, track);
		
		String xml = String.format(
			"<change><ops><add><i>%d</i><items>%s01</items></add></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,%d</version>",
			position, track.getId(), new Date().getTime() / 1000, user,
			playlist.getRevision() + 1, playlist.getTracks().size(),
			playlist.getChecksum(), playlist.isCollaborative()?1:0
		);
		
		/* Remove the track again, because we need the old checksum for sending. */
		playlist.getTracks().remove(position);
		
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
		byte[] data = callback.get();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);		
		
		/* Check for success. */
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			/* Add the track, since operation was successful. */
			playlist.getTracks().add(position, track);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Add multiple tracks to a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param tracks   A {@link List} of tracks to be added.
	 * @param position The target position of the added track.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistAddTracks(Playlist playlist, List<Track> tracks, int position){
		String user = this.session.getUsername();
		
		if(!playlist.isCollaborative() && !playlist.getAuthor().equals(user)){
			return false;
		}
		
		/* First add the tracks to calculate the new checksum. */
		playlist.getTracks().addAll(position, tracks);
		
		/* Build a comma separated list of tracks. */
		String trackList = "";
		
		for(int i = 0; i < tracks.size(); i++){
			trackList += ((i > 0)?",":"") + tracks.get(i).getId();
		}
		
		String xml = String.format(
			"<change><ops><add><i>%d</i><items>%s01</items></add></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,%d</version>",
			position, trackList, new Date().getTime() / 1000, user,
			playlist.getRevision() + 1, playlist.getTracks().size(),
			playlist.getChecksum(), playlist.isCollaborative()?1:0
		);
		
		/* Remove the tracks again, because we need the old checksum for sending. */
		for(int i = 0; i < tracks.size(); i++){
			playlist.getTracks().remove(position);
		}
		
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
		byte[] data = callback.get();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		/* Check for success. */
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			/* Add the tracks, since operation was successful. */
			playlist.getTracks().addAll(position, tracks);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Remove a track from a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param position The position of the track to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistRemoveTrack(Playlist playlist, int position){
		return this.playlistRemoveTracks(playlist, position, 1);
	}
	
	/**
	 * Remove multiple tracks from a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param position The position of the tracks to remove.
	 * @param count    The number of track to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistRemoveTracks(Playlist playlist, int position, int count){
		String user = this.session.getUsername();
		
		if(!playlist.isCollaborative() && !playlist.getAuthor().equals(user)){
			return false;
		}
		
		/* Create a sublist view (important!) and clone it by constructing a new ArrayList. */
		List<Track> tracks = new ArrayList<Track>(
			playlist.getTracks().subList(position, position + count)
		);
		
		/* First remove the track(s) to calculate the new checksum. FIXME: remove them in single steps. */
		playlist.getTracks().removeAll(tracks);
		
		String xml = String.format(
			"<change><ops><del><i>%d</i><k>%d</k></del></ops>" +
			"<time>%d</time><user>%s</user></change>" +
			"<version>%010d,%010d,%010d,%d</version>",
			position, count, new Date().getTime() / 1000, user,
			playlist.getRevision() + 1, playlist.getTracks().size(),
			playlist.getChecksum(), playlist.isCollaborative()?1:0
		);
		
		/* Add the track(s) again, because we need the old checksum for sending. */
		playlist.getTracks().addAll(position, tracks);
		
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
		byte[] data = callback.get();
		
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		/* Check for success. */
		if(playlistElement.hasChild("confirm")){
			/* Split version string into parts. */
			String[] parts = playlistElement.getChild("confirm").getChildText("version").split(",", 4);
			
			/* Set values. */
			playlist.setRevision(Long.parseLong(parts[0]));
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			/* Remove the track(s), since operation was successful. */
			playlist.getTracks().removeAll(tracks);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	// TODO: playlistMoveTrack(s) : <mov><i>6</i><j>2</j></mov>
	
	/**
	 * Rename a playlist.
	 * 
	 * @param playlist The {@link Playlist} to rename.
	 * @param name     The new name for the playlist.
	 * 
	 * @return true on success or false on failure.
	 * 
	 * @see Playlist
	 */
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
		byte[] data = callback.get();
		
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
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			playlist.setName(name);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Set playlist collaboration.
	 * 
	 * @param playlist      The {@link Playlist} to change.
	 * @param collaborative Whether it should be collaborative or not.
	 * 
	 * @return true on success or false on failure.
	 * 
	 * @see Playlist
	 */
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
		byte[] data = callback.get();
		
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
			playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
			
			if(playlist.getChecksum() != Long.parseLong(parts[2])){
				System.out.println("Checksum error!");
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Play a track in a background thread.
	 * 
	 * @param track    A {@link Track} object identifying the track to be played.
	 * @param listener A {@link PlaybackListener} receiving playback status updates.
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
		byte[] key = callback.get();
		
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
			this.player.pause();
		}
	}
	
	/**
	 * Stop playback of current track.
	 */
	public void stop(){
		if(this.player != null){
			this.player.stop();
			
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
				//System.out.println("Country: " + new String(payload, Charset.forName("UTF-8")));
				this.user.setCountry(new String(payload, Charset.forName("UTF-8")));
				
				/* Release 'country' permit. */
				this.userSemaphore.release();
				
				break;
			}
			case Command.COMMAND_P2P_INITBLK: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip 11 byte header... */
				/*System.out.println("Notification: " + new String(
					Arrays.copyOfRange(payload, 11, payload.length), Charset.forName("UTF-8")
				));*/
				this.user.setNotification(new String(
					Arrays.copyOfRange(payload, 11, payload.length), Charset.forName("UTF-8")
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				XMLElement prodinfoElement = XML.load(new String(payload, Charset.forName("UTF-8")));
				
				this.user = User.fromXMLElement(prodinfoElement, this.user);
				
				/* Release 'prodinfo' permit. */
				this.userSemaphore.release();
				
				/* Payload is uncompressed XML. */
				if(!this.user.isPremium()){
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
		JotifyConnection jotify = new JotifyConnection();
		
		/* Create a scanner. */
		Scanner scanner = new Scanner(System.in);
		
		/* Current playlist. */
		Playlist playlist = new Playlist();
		
		/* Login. */
		while(true){
			System.out.print("Username: ");
			String username = scanner.nextLine();
			
			System.out.print("Password: ");
			String password = scanner.nextLine();
			
			try{
				jotify.login(username, password);
				
				System.out.println("Logged in! Type 'help' to see available commands.");
				
				break;
			}
			catch(AuthenticationException e){
				System.out.println("Invalid username and/or password! Try again.");
			}
		}
		
		/* Start packet IO in the background. */
		new Thread(jotify, "JotifyConnection-Thread").start();
		
		/* Print user info. */
		System.out.println(jotify.user());
		
		/* Wait for commands. */
		while(true){
			String   line     = scanner.nextLine();
			String[] parts    = line.split(" ", 2);
			String   command  = parts[0];
			String   argument = (parts.length > 1)?parts[1]:null;
			
			if(command.equals("search")){
				Result result = jotify.search(argument);

				playlist = Playlist.fromResult(result.getQuery(), "jotify", result);
				
				int i = 0;
				
				for(Track track : result.getTracks()){
					System.out.format(
						"%2d | %20s - %45s | %32s\n",
						i++,
						track.getArtist().getName(),
						track.getTitle(),
						track.getId()
					);
					
					if(i == 15){
						break;
					}
				}
			}
			else if(command.equals("toplist")){
				Result result = jotify.toplist(argument, null, null);
				
				int i = 0;
				
				for(Artist artist : result.getArtists()){
					System.out.format(
						"%2d | %20s | %32s\n",
						i++,
						artist.getName(),
						artist.getId()
					);
					
					if(i == 15){
						break;
					}
				}
				
				for(Album album : result.getAlbums()){
					System.out.format(
						"%2d | %20s - %45s | %32s\n",
						i++,
						album.getArtist().getName(),
						album.getName(),
						album.getId()
					);
					
					if(i == 15){
						break;
					}
				}
				
				for(Track track : result.getTracks()){
					System.out.format(
						"%2d | %20s - %45s | %32s\n",
						i++,
						track.getArtist().getName(),
						track.getTitle(),
						track.getId()
					);
					
					if(i == 15){
						break;
					}
				}
			}
			else if(command.equals("play")){
				int position = Integer.parseInt(argument);
				
				if(position >= 0 && position < playlist.getTracks().size()){
					Result result = jotify.browse(playlist.getTracks().get(position));
					Track  track  = result.getTracks().get(0);				
					
					System.out.format("Playing: %s - %s\n", track.getArtist().getName(), track.getTitle());
					
					jotify.stop();
					jotify.play(track, null);
				}
				else{
					System.out.format("Position %d not available!\n", position);
				}
			}
			else if(command.equals("help")){
				System.out.println("Available commands:");
				System.out.println("	search <query>");
				System.out.println("	toplist <type>");
				System.out.println("	play   <id>");
				System.out.println("	quit");
			}
			else if(command.equals("quit")){
				/* Stop playing. */
				jotify.stop();
				
				/* Close connection. */
				jotify.close();
				
				break;
			}
			else{
				System.out.println("Unrecognized command!");
			}
		}
	}
}
