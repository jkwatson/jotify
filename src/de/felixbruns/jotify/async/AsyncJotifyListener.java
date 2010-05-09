package de.felixbruns.jotify.async;

import java.awt.Image;
import java.util.List;

import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.User;

public interface AsyncJotifyListener {
	public void loggedIn();
	public void loggedOut();
	
	public void receivedUserData(User user);
	
	public void receivedToplist(Result toplist, Object userdata);
	public void receivedSearchResult(Result result, Object userdata);
	public void receivedImage(Image image, Object userdata);
	public void receivedArtist(Artist artist, Object userdata);
	public void receivedAlbum(Album album, Object userdata);
	public void receivedTracks(List<Track> tracks, Object userdata);
	public void receivedReplacementTracks(List<Track> tracks, Object userdata);
	
	public void receivedPlaylistContainer(PlaylistContainer container);
	public void receivedPlaylist(Playlist playlist);
	public void receivedPlaylistUpdate(String id);
	
	public void receivedException(ConnectionException e);
	public void receivedException(ProtocolException e);
	public void receivedException(AuthenticationException e);
}
