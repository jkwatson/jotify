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
	 * @return A {@link Track} object or null on failure.
	 * 
	 * @see Track
	 */
	public Track browse(Track track);
	
	/**
	 * Browse information for multiple tracks.
	 * 
	 * @param tracks A {@link List} of {@link Track} objects identifying
	 *               the tracks to browse.
	 * 
	 * @return A list of {@link Track} objects or null on failure.
	 * 
	 * @see Track
	 */
	public List<Track> browse(List<Track> tracks);
	
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
	 * @return A {@link Track} object or null on failure.
	 * 
	 * @see Track
	 */
	public Track browseTrack(String id);
	
	/**
	 * Browse information for multiple tracks by id.
	 * 
	 * @param ids A {@link List} of ids identifying the tracks to browse.
	 * 
	 * @return A list of {@link Track} objects or null on failure.
	 * 
	 * @see Track
	 */
	public List<Track> browseTracks(List<String> ids);
	
	/**
	 * Get stored user playlists.
	 * 
	 * @return A {@link PlaylistContainer} holding {@link Playlist} objects
	 *         or an empty {@link PlaylistContainer} on failure.
	 *         Note: {@link Playlist} objects only hold id and author and need
	 *         to be loaded using {@link #playlist(String)}.
	 * 
	 * @see PlaylistContainer
	 */
	public PlaylistContainer playlistContainer();
	
	/**
	 * Add a playlist to a playlist container.
	 * 
	 * @param playlistContainer A {@link PlaylistContainer} to add the playlist to.
	 * @param playlist          The {@link Playlist} to be added.
	 * 
	 * @return true on success and false on failure.
	 * 
	 * @see PlaylistContainer
	 */
	public boolean playlistContainerAddPlaylist(PlaylistContainer playlistContainer, Playlist playlist);
	
	/**
	 * Add a playlist to a playlist container.
	 * 
	 * @param playlistContainer The playlist container.
	 * @param playlist          The playlist to be added.
	 * @param position          The target position of the added playlist.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistContainerAddPlaylist(PlaylistContainer playlistContainer, Playlist playlist, int position);
	
	/**
	 * Add multiple playlists to a playlist container.
	 * 
	 * @param playlistContainer The playlist container.
	 * @param playlists         A {@link List} of playlists to be added.
	 * @param position          The target position of the added playlists.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistContainerAddPlaylists(PlaylistContainer playlistContainer, List<Playlist> playlists, int position);
	
	/**
	 * Remove a playlist from a playlist container.
	 * 
	 * @param playlistContainer The playlist container.
	 * @param position          The position of the playlist to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistContainerRemovePlaylist(PlaylistContainer playlistContainer, int position);
	
	/**
	 * Remove multiple playlists from a playlist container.
	 * 
	 * @param playlistContainer The playlist container.
	 * @param position          The position of the tracks to remove.
	 * @param count             The number of track to remove.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistContainerRemovePlaylists(PlaylistContainer playlistContainer, int position, int count);
	
	/**
	 * Get a playlist.
	 * 
	 * @param id     Id of the playlist to load.
	 * @param cached Whether to use a cached version if available or not.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlist(String id, boolean cached);
	
	/**
	 * Get a playlist.
	 * 
	 * @param id Id of the playlist to load.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlist(String id);
	
	/**
	 * Create a playlist.
	 * 
	 * @param name          The name of the playlist to create.
	 * @param collaborative If the playlist shall be collaborative.
	 * @param description   A description of the playlist.
	 * @param picture       An image id to associate with this playlist.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlistCreate(String name, boolean collaborative, String description, String picture);
	
	/**
	 * Create a playlist.
	 * 
	 * @param name The name of the playlist to create.
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 */
	public Playlist playlistCreate(String name);
	
	/**
	 * Create a playlist from a given album.
	 * 
	 * @param sourceAlbum An {@link Album} object
	 * 
	 * @return A {@link Playlist} object or null on failure.
	 * 
	 * @see Playlist
	 * @see Album
	 */
	public Playlist playlistCreate(Album sourceAlbum);
	
	/**
	 * Destroy a playlist (Note: It will not be destroyed on the server immediately).
	 * 
	 * @param playlist The playlist to destroy.
	 * 
	 * @return true if the playlist was successfully destroyed, false otherwise.
	 * 
	 * @see Playlist
	 */
	public boolean playlistDestroy(Playlist playlist);
	
	/**
	 * Add a track to a playlist.
	 * 
	 * @param playlist The playlist.
	 * @param track    The track to be added.
	 * 
	 * @return true on success and false on failure.
	 */
	public boolean playlistAddTrack(Playlist playlist, Track track);
	
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
	
	/**
	 * Set playlist information.
	 * 
	 * @param playlist    The {@link Playlist} to change.
	 * @param description The description to set.
	 * @param picture     The picture to set.
	 * 
	 * @return true on success or false on failure.
	 * 
	 * @see Playlist
	 */
	public boolean playlistSetInformation(Playlist playlist, String description, String picture);
}
