package de.felixbruns.jotify.util;

import java.util.zip.Adler32;

import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Track;

public class SpotifyChecksum extends Adler32 {
	public void update(Playlist playlist){
		this.update(Hex.toBytes(playlist.getId()));
		this.update((byte)0x02);
	}
	
	public void update(Artist artist){
		this.update(Hex.toBytes(artist.getId()));
		this.update((byte)0x02); // TODO: is it really 0x02?
	}
	
	public void update(Album album){
		this.update(Hex.toBytes(album.getId()));
		this.update((byte)0x02); // TODO: is it really 0x02?
	}
	
	public void update(Track track){
		this.update(Hex.toBytes(track.getId()));
		this.update((byte)0x01);
	}
}
