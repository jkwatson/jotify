package de.felixbruns.jotify.util;

import java.util.zip.Adler32;

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
	 * Update the checksum with a {@link Track}.
	 * 
	 * @param track A {@link Track} object.
	 */
	public void update(Track track){
		this.update(Hex.toBytes(track.getId()));
		this.update((byte)0x01);
	}
}
