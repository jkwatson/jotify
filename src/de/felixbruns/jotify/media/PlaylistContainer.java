package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PlaylistContainer implements Iterable<Playlist> {
	public static final PlaylistContainer EMPTY = new PlaylistContainer();
	
	private String         author;
	private List<Playlist> playlists;
	private long           revision;
	private long           checksum;
	
	public PlaylistContainer(){
		this.author    = null;
		this.playlists = new ArrayList<Playlist>();
		this.revision  = -1;
		this.checksum  = -1;
	}
	
	public String getAuthor(){
		return this.author;
	}
	
	public void setAuthor(String author){
		this.author = author;
	}
	
	public List<Playlist> getPlaylists(){
		return this.playlists;
	}
	
	public void setPlaylists(List<Playlist> playlists){
		this.playlists = playlists;
	}
	
	public long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	/**
	 * Get and update the checksum of this playlist container.
	 * 
	 * @return The current checksum.
	 */
	public long getChecksum(){
		Checksum checksum = new Checksum(); 
		
		for(Playlist playlist : this.playlists){
			checksum.update(playlist);
		}
		
		this.checksum = checksum.getValue();
		
		return this.checksum;
	}
	
	/**
	 * Set the current checksum of this playlist container.
	 * 
	 * @param checksum The checksum.
	 */
	public void setChecksum(long checksum){
		this.checksum = checksum;
	}
	
	public Iterator<Playlist> iterator(){
		return this.playlists.iterator();
	}
	
	public String toString(){
		return String.format("[PlaylistContainer: %s, %d]", this.author, this.revision);
	}
}
