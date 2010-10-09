package de.felixbruns.jotify.gui.listeners;

import de.felixbruns.jotify.media.Playlist;

public interface PlaylistListener {
	public void playlistAdded(Playlist playlist);
	public void playlistUpdated(Playlist playlist);
	public void playlistRemoved(Playlist playlist);
	public void playlistSelected(Playlist playlist);
}
