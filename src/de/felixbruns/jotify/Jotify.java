package de.felixbruns.jotify;

import java.awt.Image;
import java.util.List;

import de.felixbruns.jotify.exceptions.*;
import de.felixbruns.jotify.media.*;
import de.felixbruns.jotify.player.Player;

public interface Jotify extends Runnable, Player {
	/**
	 * Login to Spotify using the specified username and password.
	 * 
	 * @param username Username to use.
	 * @param password Corresponding password.
	 * 
	 * @throws ConnectionException
	 * @throws AuthenticationException
	 */
	public void login(String username, String password) throws ConnectionException, AuthenticationException;
	
	/**
	 *  Closes the connection to a Spotify server.
	 *  
	 *  @throws ConnectionException
	 */
	public void close() throws ConnectionException;
	
	/**
	 * Get user info.
	 * 
	 * @return A {@link User} object.
	 * 
	 * @throws IllegalStateException
	 * 
	 * @see User
	 */
	public User user();
	
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
	public Result toplist(String type, String region, String username);
	
	/**
	 * Search for an artist, album or track.
	 * 
	 * @param query Your search query.
	 * 
	 * @return A {@link Result} object.
	 * 
	 * @see Result
	 */
	public Result search(String query);
	
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
	public Image image(String id);
	
	/**
	 * Browse artist info.
	 * 
	 * @param artist An {@link Artist} object identifying the artist to browse.
	 * 
	 * @return A new {@link Artist} object holding more information about
	 *         the artist or null on failure.
	 * 
	 * @see Artist
	 */
	public Artist browse(Artist artist);
	
	/**
	 * Browse album info.
	 * 
	 * @param album An {@link Album} object identifying the album to browse.
	 * 
	 * @return A new {@link Album} object holding more information about
	 *         the album or null on failure.
	 * 
	 * @see Album
	 */
	public Album browse(Album album);
	
	/**
	 * Browse track info.
	 * 
	 * @param album A {@link Track} object identifying the track to browse.
	 * 
	 * @return A {@link Result} object holding more information about
	 *         the track or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browse(Track track);
	
	/**
	 * Browse information for multiple tracks.
	 * 
	 * @param tracks A {@link List} of {@link Track} objects identifying
	 *               the tracks to browse.
	 * 
	 * @return A {@link Result} object holding more information about
	 *         the tracks or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browse(List<Track> tracks);
	
	/**
	 * Browse artist info by id.
	 * 
	 * @param id An id identifying the artist to browse.
	 * 
	 * @return An {@link Artist} object holding more information about
	 *         the artist or null on failure.
	 * 
	 * @see Artist
	 */
	public Artist browseArtist(String id);
	
	/**
	 * Browse album info by id.
	 * 
	 * @param id An id identifying the album to browse.
	 * 
	 * @return An {@link Album} object holding more information about
	 *         the album or null on failure.
	 * 
	 * @see Album
	 */
	public Album browseAlbum(String id);
	
	/**
	 * Browse track info by id.
	 * 
	 * @param id An id identifying the track to browse.
	 * 
	 * @return A {@link Result} object holding more information about
	 *         the track or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browseTrack(String id);
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param tracks A {@link List} of ids identifying the tracks to browse.
	 * 
	 * @return A {@link Result} object holding more information about
	 *         the tracks or null on failure.
	 * 
	 * @see Track
	 * @see Result
	 */
	public Result browseTracks(List<String> tracks);
	
	/**
	 * Get a list of stored playlists.
	 * 
	 * @return A {@link List} of {@link Playlist} objects or null on failure.
	 *         (Note: {@link Playlist} objects only hold id and author)
	 * 
	 * @see Playlist
	 */
	public PlaylistContainer playlists();
	
	/**
	 * Add a playlist to the end of the list of stored playlists.
	 * 
	 * @param playlists A {@link PlaylistContainer} to add the playlist to.
	 * @param playlist  The {@link Playlist} to be added.
	 * 
	 * @return true on success and false on failure.
	 * 
	 * @see PlaylistContainer
	 */
	public boolean playlistsAddPlaylist(PlaylistContainer playlists, Playlist playlist);
	
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
	public boolean playlistsAddPlaylist(PlaylistContainer playlists, Playlist playlist, int position);
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlist(String id, boolean cached);
	public Playlist playlist(String id);
	
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
	public Playlist playlistCreate(String name, boolean collaborative);
	
	/**
	 * Add a track to a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param track    The track to be added.
	 * @param position The target position of the added track.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistAddTrack(Playlist playlist, Track track, int position);
	
	/**
	 * Add multiple tracks to a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param tracks   A {@link List} of tracks to be added.
	 * @param position The target position of the added track.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistAddTracks(Playlist playlist, List<Track> tracks, int position);
	
	/**
	 * Remove a track from a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param position The position of the track to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistRemoveTrack(Playlist playlist, int position);
	
	/**
	 * Remove multiple tracks from a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param position The position of the tracks to remove.
	 * @param count    The number of track to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistRemoveTracks(Playlist playlist, int position, int count);
	
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
	public boolean playlistRename(Playlist playlist, String name);
	
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
	public boolean playlistSetCollaborative(Playlist playlist, boolean collaborative);
}
