package de.felixbruns.jotify.async;

import java.awt.Image;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import de.felixbruns.jotify.cache.*;
import de.felixbruns.jotify.crypto.*;
import de.felixbruns.jotify.exceptions.*;
import de.felixbruns.jotify.media.*;
import de.felixbruns.jotify.media.Link.InvalidSpotifyURIException;
import de.felixbruns.jotify.media.parser.XMLMediaParser;
import de.felixbruns.jotify.media.parser.XMLPlaylistParser;
import de.felixbruns.jotify.media.parser.XMLUserParser;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;
import de.felixbruns.jotify.player.SpotifyOggPlayer;
import de.felixbruns.jotify.protocol.*;
import de.felixbruns.jotify.protocol.channel.*;
import de.felixbruns.jotify.util.*;

public class AsyncJotifyConnection implements AsyncJotify, CommandListener {
	/*
	 * Values for browsing media.
	 */
	private static final int BROWSE_ARTIST = 1;
	private static final int BROWSE_ALBUM  = 2;
	private static final int BROWSE_TRACK  = 3;
	
	/*
	 * Session and protocol associated with this connection.
	 */
	protected Session  session;
	protected Protocol protocol;
	
	/*
	 * User information.
	 */
	private String username;
	private String password;
	private User   user;
	
	/*
	 * Player and cache.
	 */
	private Player player;
	private Cache  cache;
	
	/**
	 * Event listeners to notify when events occur
	 * (login, logout, data, exceptions).
	 */
	private List<AsyncJotifyListener> listeners;
	
	/**
	 * Create a new asynchronus Jotify instance using the default
	 * {@link Cache} implementation.
	 */
	public AsyncJotifyConnection(){
		this(new FileCache());
	}
	
	/**
	 * Create a new asynchronous Jotify instance using a specified
	 * {@link Cache} implementation.
	 * 
	 * @param cache Cache implementation to use.
	 * 
	 * @see MemoryCache
	 * @see FileCache
	 */
	public AsyncJotifyConnection(Cache cache){
		this.session   = new Session();
		this.protocol  = null;
		this.username  = null;
		this.password  = null;
		this.user      = null;
		this.cache     = cache;
		this.player    = null;
		this.listeners = new LinkedList<AsyncJotifyListener>();
	}
	
	/**
	 * Add a listener to receive events.
	 * 
	 * @param listener The {@link AsyncJotifyListener} to add.
	 */
	public void addListener(AsyncJotifyListener listener){
		this.listeners.add(listener);
	}
	
	/**
	 * Login to Spotify using the specified username and password.
	 * 
	 * @param username Username to use.
	 * @param password Corresponding password.
	 */
	public void login(String username, String password){
		/* Check if we're already logged in. */
		if(this.protocol != null){
			throw new IllegalStateException("Already logged in!");
		}
		
		/* Set username and password. */
		this.username = username;
		this.password = password;
		
		/* Create account info object. */
		this.user = new User(username);
		
		/* Start login and I/O thread. */
		new Thread(this, "I/O-Thread").start();
	}
	
	/**
	 * Closes the connection to the Spotify server.
	 */
	public void logout(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* This will make receivePacket return immediately. */
		try{
			this.protocol.disconnect();
		}
		catch(ConnectionException e){
			/* We don't really care... */
		}
		
		/* Reset protocol to 'null'. */
		this.protocol = null;
		
		/* Fire logged out event. */
		for(AsyncJotifyListener listener : this.listeners){
			listener.loggedOut();
		}
	}
	
	/**
	 * Continuously receives packets in order to handle them.
	 */
	public void run(){
		/* Authenticate session and get protocol. */
		try{
			this.protocol = this.session.authenticate(this.username, this.password);
		}
		catch(ConnectionException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
			
			return;
		}
		catch(AuthenticationException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
			
			return;
		}
		
		/* Create OGG player. */
		this.player = new SpotifyOggPlayer(this.protocol);
		
		/* Add command handler. */
		this.protocol.addListener(this);
		
		/* Fire logged in event. */
		for(AsyncJotifyListener listener : this.listeners){
			listener.loggedIn();
		}
		
		/* Continuously receive packets until connection is closed. */
		try{
			while(true){
				this.protocol.receivePacket();
			}
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Request a toplist.<br><br>
	 * 
	 * <b>Examples:</b>
	 * <ul>
	 *     <li>{@code requestToplist("artist", "SE", null)} - Artist toplist (in Sweden)</li>
	 *     <li>{@code requestToplist("album", null, "user")} - Album toplist (for "user")</li>
	 *     <li>{@code requestToplist("track", null, null)} - Track toplist (everywhere)</li>
	 * </ul>
	 * 
	 * @param type     A toplist type. e.g. "artist", "album" or "track".
	 * @param region   A region code or {@code null}. e.g. "SE" or "DE".
	 * @param username A username or {@code null}.
	 * 
	 * @see #requestToplist(String, String, String, Object)
	 */
	public void requestToplist(String type, String region, String username){
		this.requestToplist(type, region, username, null);
	}
	
	/**
	 * Request a toplist.
	 * 
	 * @param type     A toplist type. e.g. "artist", "album" or "track".
	 * @param region   A region code or null. e.g. "SE" or "DE".
	 * @param username A username or null.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestToplist(String, String, String)
	 * @see Result
	 */
	public void requestToplist(final String type, final String region, final String username, final Object userdata){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Create parameter map. */
		Map<String, String> params = new HashMap<String, String>();
		
		/* Add parameters. */
		params.put("type",     type);
		params.put("region",   region);
		params.put("username", username);
		
		/* Send toplist request. */
		try{
			this.protocol.sendToplistRequest(new AsyncChannelCallback(){
				public void receivedData(byte[] data){
					/* Create result from XML. */
					Result toplist = XMLMediaParser.parseResult(data, "UTF-8");
					
					/* Set query for this toplist. */
					toplist.setQuery(type + ", " + region + ", " + username);
					
					/* Fire toplist event. */
					for(AsyncJotifyListener listener : listeners){
						listener.receivedToplist(toplist, userdata);
					}
				}
			}, params);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query The search query.
	 * 
	 * @see #search(String, Object)
	 */
	public void search(String query){
		this.search(query, null);
	}
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query    The search query.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #search(String)
	 */
	public void search(final String query, final Object userdata){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Send search query. */
		try{
			this.protocol.sendSearchQuery(new AsyncChannelCallback(){
				public void receivedData(byte[] data){
					/* Create result from XML. */
					Result result = XMLMediaParser.parseResult(data, "UTF-8");
					
					/* Set search query. */
					result.setQuery(query);
					
					/* Fire search result event. */
					for(AsyncJotifyListener listener : listeners){
						listener.receivedSearchResult(result, userdata);
					}
				}
			}, query);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Get an image (e.g. artist portrait or cover) by requesting
	 * it from the server or loading it from the local cache, if
	 * available.
	 * 
	 * @param id The id of the image to load.
	 * 
	 * @see #requestImage(String, Object)
	 */
	public void requestImage(String id){
		this.requestImage(id, null);
	}
	
	/**
	 * Get an image (e.g. artist portrait or cover) by requesting
	 * it from the server or loading it from the local cache, if
	 * available.
	 * 
	 * @param id       The id of the image to load.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestImage(String)
	 */
	public void requestImage(final String id, final Object userdata){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Check cache. */
		if(this.cache != null && this.cache.contains("image", id)){
			/* Load data from cache. */
			byte[] data = this.cache.load("image", id);
			
			/* Create image. */
			try{
				Image image = ImageIO.read(new ByteArrayInputStream(data));
				
				/* Fire image event. */
				for(AsyncJotifyListener listener : listeners){
					listener.receivedImage(image, userdata);
				}
			}
			catch(IOException e){
				throw new RuntimeException(e);
			}
		}
		else{
			/* Send image request. */
			try{
				this.protocol.sendImageRequest(new AsyncChannelCallback(){
					public void receivedData(byte[] data){
						/* Save to cache. */
						if(cache != null){
							cache.store("image", id, data);
						}
						
						/* Create image. */
						try{
							Image image = ImageIO.read(new ByteArrayInputStream(data));
							
							/* Fire image event. */
							for(AsyncJotifyListener listener : listeners){
								listener.receivedImage(image, userdata);
							}
						}
						catch(IOException e){
							throw new RuntimeException(e);
						}
					}
				}, id);
			}
			catch(ProtocolException e){
				/* Fire exception event. */
				for(AsyncJotifyListener listener : this.listeners){
					listener.receivedException(e);
				}
			}
		}
	}
	
	/**
	 * Internal browse method.
	 * 
	 * @param type     The browse type (artist, album or track).
	 * @param id       The id of the media to browse.
	 * @param userdata A user object which is passed to the callback.
	 */
	private void browse(final int type, String id, final Object userdata){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/*
		 * Check if id is a 32-character hex string,
		 * if not try to parse it as a Spotify URI.
		 */
		if(id.length() != 32 && !Hex.isHex(id)){
			try{
				Link link = Link.create(id);
				
				if((type == BROWSE_ARTIST && !link.isArtistLink()) ||
				   (type == BROWSE_ALBUM  && !link.isAlbumLink())  ||
				   (type == BROWSE_TRACK  && !link.isTrackLink())){
					throw new IllegalArgumentException(
						"Browse type doesn't match given Spotify URI."
					);
				}
				
				id = link.getId();
			}
			catch(InvalidSpotifyURIException e){
				throw new IllegalArgumentException(
					"Given id is neither a 32-character " +
					"hex string nor a valid Spotify URI."
				);
			}
		}
		
		/* Send browse request. */
		try{
			this.protocol.sendBrowseRequest(new AsyncChannelCallback(){
				public void receivedData(byte[] data){
					/* Create object from XML. */
					Object object = XMLMediaParser.parse(data, "UTF-8");
					
					/* Fire browse event. */
					if(type == BROWSE_ARTIST && object instanceof Artist){
						for(AsyncJotifyListener listener : listeners){
							listener.receivedArtist((Artist)object, userdata);
						}
					}
					else if(type == BROWSE_ALBUM && object instanceof Album){
						for(AsyncJotifyListener listener : listeners){
							listener.receivedAlbum((Album)object, userdata);
						}
					}
					else if(type == BROWSE_TRACK && object instanceof Result){
						for(AsyncJotifyListener listener : listeners){
							listener.receivedTracks(((Result)object).getTracks(), userdata);
						}
					}
				}
			}, type, id);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Browse artist info by id.
	 * 
	 * @param id An id identifying the artist to browse.
	 * 
	 * @see #browse(Artist)
	 * @see #browse(Artist, Object)
	 * @see #browseArtist(String, Object)
	 */
	public void browseArtist(String id){
		this.browse(BROWSE_ARTIST, id, null);
	}
	
	/**
	 * Browse artist info by id.
	 * 
	 * @param id       An id identifying the artist to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Artist)
	 * @see #browse(Artist, Object)
	 * @see #browseArtist(String)
	 */
	public void browseArtist(String id, Object userdata){
		this.browse(BROWSE_ARTIST, id, userdata);
	}
	
	/**
	 * Browse album info by id.
	 * 
	 * @param id An id identifying the album to browse.
	 * 
	 * @see #browse(Album)
	 * @see #browse(Album, Object)
	 * @see #browseAlbum(String, Object)
	 */
	public void browseAlbum(String id){
		this.browse(BROWSE_ALBUM, id, null);
	}
	
	/**
	 * Browse album info by id.
	 * 
	 * @param id       An id identifying the album to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Album)
	 * @see #browse(Album, Object)
	 * @see #browseAlbum(String)
	 */
	public void browseAlbum(String id, Object userdata){
		this.browse(BROWSE_ALBUM, id, userdata);
	}
	
	/**
	 * Browse track info by id.
	 * 
	 * @param id An id identifying the track to browse.
	 * 
	 * @see #browse(Track)
	 * @see #browse(Track, Object)
	 * @see #browseTrack(String, Object)
	 */
	public void browseTrack(String id){
		this.browse(BROWSE_TRACK, id, null);
	}
	
	/**
	 * Browse track info by id.
	 * 
	 * @param id       An id identifying the track to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Track)
	 * @see #browse(Track, Object)
	 * @see #browseTrack(String)
	 */
	public void browseTrack(String id, Object userdata){
		this.browse(BROWSE_TRACK, id, userdata);
	}
	
	/**
	 * Browse artist info.
	 * 
	 * @param artist An {@link Artist} object identifying the artist to browse.
	 * 
	 * @see #browse(Artist, Object)
	 * @see #browseArtist(String)
	 * @see #browseArtist(String, Object)
	 */
	public void browse(Artist artist){
		this.browseArtist(artist.getId(), null);
	}
	
	/**
	 * Browse artist info.
	 * 
	 * @param artist   An {@link Artist} object identifying the artist to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Artist)
	 * @see #browseArtist(String)
	 * @see #browseArtist(String, Object)
	 */
	public void browse(Artist artist, Object userdata){
		this.browseArtist(artist.getId(), userdata);
	}
	
	/**
	 * Browse album info.
	 * 
	 * @param album An {@link Album} object identifying the album to browse.
	 * 
	 * @see #browse(Album, Object)
	 * @see #browseAlbum(String)
	 * @see #browseAlbum(String, Object)
	 */
	public void browse(Album album){
		this.browseAlbum(album.getId(), null);
	}
	
	/**
	 * Browse album info.
	 * 
	 * @param album    An {@link Album} object identifying the album to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Album)
	 * @see #browseAlbum(String)
	 * @see #browseAlbum(String, Object)
	 */
	public void browse(Album album, Object userdata){
		this.browseAlbum(album.getId(), userdata);
	}
	
	/**
	 * Browse track info.
	 * 
	 * @param album A {@link Track} object identifying the track to browse.
	 * 
	 * @see #browse(Track, Object)
	 * @see #browseTrack(String)
	 * @see #browseTrack(String, Object)
	 */
	public void browse(Track track){
		this.browseTrack(track.getId(), null);
	}
	
	/**
	 * Browse track info.
	 * 
	 * @param album    A {@link Track} object identifying the track to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(Track)
	 * @see #browseTrack(String)
	 * @see #browseTrack(String, Object)
	 */
	public void browse(Track track, Object userdata){
		this.browseTrack(track.getId(), userdata);
	}
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param ids A {@link List} of ids identifying the tracks to browse.
	 * 
	 * @see #browse(List)
	 * @see #browse(List, Object)
	 * @see #browseTracks(List, Object)
	 */
	public void browseTracks(final List<String> ids){
		this.browseTracks(ids, null);
	}
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param ids      A {@link List} of ids identifying the tracks to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(List)
	 * @see #browse(List, Object)
	 * @see #browseTracks(List)
	 */
	public void browseTracks(final List<String> ids, final Object userdata){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Create cache hash. */
		StringBuffer hashBuffer = new StringBuffer();
		
		for(int i = 0; i < ids.size(); i++){
			String id = ids.get(i);
			
			/*
			 * Check if id is a 32-character hex string,
			 * if not try to parse it as a Spotify URI.
			 */
			if(id.length() != 32 && !Hex.isHex(id)){
				try{
					Link link = Link.create(id);
					
					if(!link.isTrackLink()){
						throw new IllegalArgumentException(
							"Browse type doesn't match given Spotify URI."
						);
					}
					
					id = link.getId();
					
					/* Set parsed id in list. */
					ids.set(i, id);
				}
				catch(InvalidSpotifyURIException e){
					throw new IllegalArgumentException(
						"Given id is neither a 32-character " +
						"hex string nor a valid Spotify URI."
					);
				}
			}
			
			/* Append id to buffer in order to create a cache hash. */
			hashBuffer.append(id);
		}
		
		final String hash = Hex.toHex(Hash.sha1(Hex.toBytes(hashBuffer.toString())));
		
		/* Check cache. */
		if(this.cache != null && this.cache.contains("browse", hash)){
			/* Load data from cache. */
			byte[] data = this.cache.load("browse", hash);
			
			/* Create object from XML. */
			Result result = XMLMediaParser.parseResult(data, "UTF-8");
			
			/* Fire browse event. */
			for(AsyncJotifyListener listener : listeners){
				listener.receivedTracks(result.getTracks(), userdata);
			}
		}
		else{
			/* Send browse request. */
			try{
				this.protocol.sendBrowseRequest(new AsyncChannelCallback(){
					public void receivedData(byte[] data){
						/* Save to cache. */
						if(cache != null){
							cache.store("browse", hash, data);
						}
						
						/* Create object from XML. */
						Result result = XMLMediaParser.parseResult(data, "UTF-8");
						
						/* Fire browse event. */
						for(AsyncJotifyListener listener : listeners){
							listener.receivedTracks(result.getTracks(), userdata);
						}
					}
				}, BROWSE_TRACK, ids);
			}
			catch(ProtocolException e){
				/* Fire exception event. */
				for(AsyncJotifyListener listener : this.listeners){
					listener.receivedException(e);
				}
			}
		}
	}
	
	/**
	 * Browse information for multiple tracks.
	 * 
	 * @param tracks A {@link List} of {@link Track} objects identifying
	 *               the tracks to browse.
	 * 
	 * @see #browse(List, Object)
	 * @see #browseTracks(List)
	 * @see #browseTracks(List, Object)
	 */
	public void browse(List<Track> tracks){
		this.browse(tracks, null);
	}
	
	/**
	 * Browse information for multiple tracks.
	 * 
	 * @param tracks   A {@link List} of {@link Track} objects identifying
	 *                 the tracks to browse.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #browse(List)
	 * @see #browseTracks(List)
	 * @see #browseTracks(List, Object)
	 */
	public void browse(List<Track> tracks, Object userdata){
		/* Create id list. */
		List<String> ids = new ArrayList<String>();
			
		for(Track track : tracks){
			ids.add(track.getId());
		}
		
		this.browseTracks(ids, userdata);
	}
	
	/**
	 * Request a replacement for a given track.
	 * 
	 * @param track A {@link Track} to request a replacement for.
	 * 
	 * @see #requestReplacement(Track, Object)
	 */
	public void requestReplacement(final Track track){
		this.requestReplacement(Arrays.asList(track), null);
	}
	
	/**
	 * Request a replacement for a given track.
	 * 
	 * @param track    A {@link Track} to request a replacement for.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestReplacement(Track)
	 */
	public void requestReplacement(final Track track, Object userdata){
		this.requestReplacement(Arrays.asList(track), userdata);
	}
	
	/**
	 * Request replacement tracks for a list of tracks.
	 * 
	 * @param tracks A list of {@link Track} objects to request replacements for.
	 * 
	 * @see #requestReplacement(List, Object)
	 */
	public void requestReplacement(List<Track> tracks){
		this.requestReplacement(tracks, null);
	}
	
	/**
	 * Request replacement tracks for a list of tracks.
	 * 
	 * @param tracks   A list of {@link Track} objects to request replacements for.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestReplacement(List)
	 */
	public void requestReplacement(List<Track> tracks, final Object userdata){
		try{
			/* Send replacement request. */
			this.protocol.sendReplacementRequest(new AsyncChannelCallback(){
				public void receivedData(byte[] data){
					/* Create object from XML. */
					Result result = XMLMediaParser.parseResult(data, "UTF-8");
					
					/* Fire browse event. */
					for(AsyncJotifyListener listener : listeners){
						listener.receivedReplacementTracks(result.getTracks(), userdata);
					}
				}
			}, tracks);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Request the list of stored playlists.
	 * 
	 * @see #requestPlaylist(String)
	 */
	public void requestPlaylistContainer(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Send playlist container request. */
		try{
			this.protocol.sendPlaylistRequest(new AsyncChannelCallback(){
				public void receivedData(byte[] data){
					/* Create PlaylistContainer. */
					PlaylistContainer playlists = XMLPlaylistParser.parsePlaylistContainer(data, "UTF-8");
					
					/* Fire search result event. */
					for(AsyncJotifyListener listener : listeners){
						listener.receivedPlaylistContainer(playlists);
					}
				}
			}, null);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener listener : this.listeners){
				listener.receivedException(e);
			}
		}
	}
	
	/**
	 * Request a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @see #requestPlaylistContainer()
	 */
	public void requestPlaylist(String id){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/*
		 * Check if id is a 32-character hex string,
		 * if not try to parse it as a Spotify URI.
		 */
		if(id.length() != 32 && !Hex.isHex(id)){
			try{
				Link link = Link.create(id);
				
				if(!link.isPlaylistLink()){
					throw new IllegalArgumentException(
						"Given Spotify URI is not a playlist URI."
					);
				}
				
				id = link.getId();
			}
			catch(InvalidSpotifyURIException e){
				throw new IllegalArgumentException(
					"Given id is neither a 32-character " +
					"hex string nor a valid Spotify URI."
				);
			}
		}
		
		/* 
		 * Check cache (currently disabled).
		 * 
		 * TODO: Work out a good playlist syncing system.
		 *       See todo in Protocol#sendPlaylistRequest.
		 */
		if(false && this.cache != null && this.cache.contains("playlist", id)){
			/* Load data from cache. */
			byte[] data = this.cache.load("playlist", id);
			
			/* Create playlist. */
			Playlist playlist = XMLPlaylistParser.parsePlaylist(data, "UTF-8", id);
			
			/* Fire playlist event. */
			for(AsyncJotifyListener listener : listeners){
				listener.receivedPlaylist(playlist);
			}
		}
		else{
			final String finalId = id;
			
			/* Send playlist request. */
			try{
				this.protocol.sendPlaylistRequest(new AsyncChannelCallback(){
					public void receivedData(byte[] data){
						/* Save to cache. */
						if(cache != null){
							cache.store("playlist", finalId, data);
						}
						
						/* Create playlist. */
						Playlist playlist = XMLPlaylistParser.parsePlaylist(data, "UTF-8", finalId);
						
						/* Fire playlist event. */
						for(AsyncJotifyListener listener : listeners){
							listener.receivedPlaylist(playlist);
						}
					}
				}, id);
			}
			catch(ProtocolException e){
				/* Fire exception event. */
				for(AsyncJotifyListener listener : this.listeners){
					listener.receivedException(e);
				}
			}
		}
	}
	
	public void play(Track track, int bitrate, PlaybackListener listener) throws TimeoutException, IOException, LineUnavailableException {
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		try{
			this.protocol.sendPlayRequest();
			
			this.player.stop();
			this.player.play(track, bitrate, listener);
		}
		catch(ProtocolException e){
			/* Fire exception event. */
			for(AsyncJotifyListener l : this.listeners){
				l.receivedException(e);
			}
		}
	}
	
	public void play(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		this.player.play();
	}
	
	public void pause(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		this.player.pause();
	}
	
	public void stop(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		this.player.stop();
	}
	
	public int length(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		return this.player.length();
	}
	
	public int position(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		return this.player.position();
	}
	
	public void seek(int ms) throws IOException {
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		this.player.seek(ms);
	}
	
	public float volume(){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		return this.player.volume();
	}
	
	public void volume(float volume){
		/* Check if we're logged in. */
		if(this.protocol == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		this.player.volume(volume);
	}
	
	/**
	 * Handles incoming commands from the server.
	 * 
	 * @param command A command.
	 * @param data    Payload of packet.
	 */
	public void commandReceived(int command, byte[] data){
		switch(command){
			case Command.COMMAND_SECRETBLK: {
				/* Check length. */
				if(data.length != 336){
					System.err.format("Got command 0x02 with len %d, expected 336!\n", data.length);
				}
				
				/* Check RSA public key. */
				byte[] rsaPublicKey = RSA.keyToBytes(this.session.getRSAPublicKey());
				
				for(int i = 0; i < 128; i++){
					if(data[16 + i] != rsaPublicKey[i]){
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
			case Command.COMMAND_PONGACK: {
				break;
			}
			case Command.COMMAND_CHANNELDATA: {
				Channel.process(data);
				
				break;
			}
			case Command.COMMAND_CHANNELERR: {
				Channel.error(data);
				
				break;
			}
			case Command.COMMAND_AESKEY: {
				/* Channel id is at offset 2. AES Key is at offset 4. */
				Channel.process(Arrays.copyOfRange(data, 2, data.length));
				
				break;
			}
			case Command.COMMAND_SHAHASH: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_COUNTRYCODE: {
				this.user.setCountry(new String(data, Charset.forName("UTF-8")));
				
				break;
			}
			case Command.COMMAND_P2P_INITBLK: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip 11 byte header... */
				this.user.setNotification(new String(
					Arrays.copyOfRange(data, 11, data.length), Charset.forName("UTF-8")
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				this.user = XMLUserParser.parseUser(data, "UTF-8", this.user); 
				
				/* Fire user data event. */
				for(AsyncJotifyListener listener : this.listeners){
					listener.receivedUserData(this.user);
				}
				
				break;
			}
			case Command.COMMAND_WELCOME: {
				break;
			}
			case Command.COMMAND_PAUSE: {
				/* TODO: Show notification and pause. */
				
				break;
			}
			case Command.COMMAND_PLAYLISTCHANGED: {
				String id = Hex.toHex(data);
				
				/* Fire playlist changed event. */
				for(AsyncJotifyListener listener : this.listeners){
					listener.receivedPlaylistUpdate(id);
				}
				
				break;
			}
			default: {
				/*System.out.format("Command: 0x%02x Length: %d\n", command, data.length);
				System.out.println("Hex:   " + Hex.toHex(data));
				System.out.println("Bytes: " + new String(data));*/
			}
		}
	}
}
