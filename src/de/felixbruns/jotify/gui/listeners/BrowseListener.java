package de.felixbruns.jotify.gui.listeners;

import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Result;

public interface BrowseListener {
	public void browsedArtist(Artist artist);
	public void browsedAlbum(Album album);
	public void browsedTracks(Result result);
}
