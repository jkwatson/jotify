package de.felixbruns.jotify.util;

import java.util.zip.Adler32;

import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Track;

/**
 * Subclass of {@link Adler32}, supplying methods to update the
 * checksum with different media objects.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class SpotifyChecksum extends Adler32 {
	/**
	 * Update the checksum with a {@link Playlist}.
	 * 
	 * @param playlist A {@link Playlist} object.
	 */
	public void update(Playlist playlist){
		this.update(Hex.toBytes(playlist.getId()));
		this.update((byte)0x02);
	}
	
	/**
	 * Update the checksum with an {@link Artist}.
	 * 
	 * @param artist An {@link Artist} object.
	 */
	public void update(Artist artist){
		this.update(Hex.toBytes(artist.getId()));
		this.update((byte)0x02); // TODO: is it really 0x02?
	}
	
	/**
	 * Update the checksum with an {@link Album}.
	 * 
	 * @param album An {@link Album} object.
	 */
	public void update(Album album){
		this.update(Hex.toBytes(album.getId()));
		this.update((byte)0x02); // TODO: is it really 0x02?
	}
	
	/**
	 * Update the checksum with a {@link Track}.
	 * 
	 * @param track A {@link Track} object.
	 */
	public void update(Track track){
		this.update(Hex.toBytes(track.getId()));
		this.update((byte)0x01);
	}
}
