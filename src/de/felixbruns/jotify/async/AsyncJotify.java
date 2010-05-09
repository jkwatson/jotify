package de.felixbruns.jotify.async;

import java.util.List;

import de.felixbruns.jotify.media.*;
import de.felixbruns.jotify.player.Player;

public interface AsyncJotify extends Runnable, Player {
	/**
	 * Add a listener to receive events.
	 * 
	 * @param listener The {@link AsyncJotifyListener} to add.
	 */
	public void addListener(AsyncJotifyListener listener);
	
	/**
	 * Login to Spotify using the specified username and password.
	 * 
	 * @param username Username to use.
	 * @param password Corresponding password.
	 */
	public void login(String username, String password);
	
	/**
	 * Closes the connection to the Spotify server.
	 */
	public void logout();
	
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
	public void requestToplist(String type, String region, String username);
	
	/**
	 * Request a toplist.
	 * 
	 * @param type     A toplist type. e.g. "artist", "album" or "track".
	 * @param region   A region code or null. e.g. "SE" or "DE".
	 * @param username A username or null.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestToplist(String, String, String)
	 */
	public void requestToplist(String type, String region, String username, Object userdata);
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query The search query.
	 * 
	 * @see #search(String, Object)
	 */
	public void search(String query);
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query    The search query.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #search(String)
	 */
	public void search(String query, Object userdata);
	
	/**
	 * Get an image (e.g. artist portrait or cover) by requesting
	 * it from the server or loading it from the local cache, if
	 * available.
	 * 
	 * @param id The id of the image to load.
	 * 
	 * @see #requestImage(String, Object)
	 */
	public void requestImage(String id);
	
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
	public void requestImage(String id, Object userdata);
	
	/**
	 * Browse artist info.
	 * 
	 * @param artist An {@link Artist} object identifying the artist to browse.
	 * 
	 * @see #browse(Artist, Object)
	 * @see #browseArtist(String)
	 * @see #browseArtist(String, Object)
	 */
	public void browse(Artist artist);
	
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
	public void browse(Artist artist, Object userdata);	
	
	/**
	 * Browse album info.
	 * 
	 * @param album An {@link Album} object identifying the album to browse.
	 * 
	 * @see #browse(Album, Object)
	 * @see #browseAlbum(String)
	 * @see #browseAlbum(String, Object)
	 */
	public void browse(Album album);
	
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
	public void browse(Album album, Object userdata);
	
	/**
	 * Browse track info.
	 * 
	 * @param album A {@link Track} object identifying the track to browse.
	 * 
	 * @see #browse(Track, Object)
	 * @see #browseTrack(String)
	 * @see #browseTrack(String, Object)
	 */
	public void browse(Track track);
	
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
	public void browse(Track track, Object userdata);
	
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
	public void browse(List<Track> tracks);
	
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
	public void browse(List<Track> tracks, Object userdata);
	
	/**
	 * Browse artist info by id.
	 * 
	 * @param id An id identifying the artist to browse.
	 * 
	 * @see #browse(Artist)
	 * @see #browse(Artist, Object)
	 * @see #browseArtist(String, Object)
	 */
	public void browseArtist(String id);
	
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
	public void browseArtist(String id, Object userdata);
	
	/**
	 * Browse album info by id.
	 * 
	 * @param id An id identifying the album to browse.
	 * 
	 * @see #browse(Album)
	 * @see #browse(Album, Object)
	 * @see #browseAlbum(String, Object)
	 */
	public void browseAlbum(String id);
	
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
	public void browseAlbum(String id, Object userdata);
	
	/**
	 * Browse track info by id.
	 * 
	 * @param id An id identifying the track to browse.
	 * 
	 * @see #browse(Track)
	 * @see #browse(Track, Object)
	 * @see #browseTrack(String, Object)
	 */
	public void browseTrack(String id);
	
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
	public void browseTrack(String id, Object userdata);
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param ids A {@link List} of ids identifying the tracks to browse.
	 * 
	 * @see #browse(List)
	 * @see #browse(List, Object)
	 * @see #browseTracks(List, Object)
	 */
	public void browseTracks(List<String> ids);
	
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
	public void browseTracks(List<String> ids, Object userdata);
	
	/**
	 * Request a replacement for a given track.
	 * 
	 * @param track A {@link Track} to request a replacement for.
	 * 
	 * @see #requestReplacement(Track, Object)
	 */
	public void requestReplacement(Track track);
	
	/**
	 * Request a replacement for a given track.
	 * 
	 * @param track    A {@link Track} to request a replacement for.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestReplacement(Track)
	 */
	public void requestReplacement(Track track, Object userdata);
	
	/**
	 * Request replacement tracks for a list of tracks.
	 * 
	 * @param tracks A list of {@link Track} objects to request replacements for.
	 * 
	 * @see #requestReplacement(List, Object)
	 */
	public void requestReplacement(List<Track> tracks);
	
	/**
	 * Request replacement tracks for a list of tracks.
	 * 
	 * @param tracks   A list of {@link Track} objects to request replacements for.
	 * @param userdata A user object which is passed to the callback.
	 * 
	 * @see #requestReplacement(List)
	 */
	public void requestReplacement(List<Track> tracks, Object userdata);
	
	/**
	 * Request the list of stored playlists.
	 * 
	 * @see #requestPlaylist(String)
	 */
	public void requestPlaylistContainer();
	
	/**
	 * Request a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @see #requestPlaylistContainer()
	 */
	public void requestPlaylist(String id);
}
