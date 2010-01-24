package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.felixbruns.jotify.util.SpotifyChecksum;
import de.felixbruns.jotify.util.SpotifyURI;

/**
 * Holds information about a playlist.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Playlist implements Iterable<Track> {
	private String      id;
	private String      name;
	private String      author;
	private List<Track> tracks;
	private long        revision;
	private long        checksum;
	private boolean     collaborative;
	private String      description;
	private String      picture;
	
	public Playlist(){
		this(null, null, null, false);
	}
	
	public Playlist(String id){
		this(id, null, null, false);
	}
	
	public Playlist(String id, String name, String author, boolean collaborative){
		this.id            = id;
		this.name          = name;
		this.author        = author;
		this.tracks        = new ArrayList<Track>();
		this.revision      = -1;
		this.checksum      = -1;
		this.collaborative = collaborative;
		this.description   = null;
		this.picture         = null;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getAuthor(){
		return this.author;
	}
	
	public void setAuthor(String author){
		this.author = author;
	}
	
	public List<Track> getTracks(){
		return this.tracks;
	}
	
	public void setTracks(List<Track> tracks){
		this.tracks = tracks;
	}
	
	public boolean hasTracks(){
		return !this.tracks.isEmpty();
	}
	
	public long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	public boolean isCollaborative(){
		return this.collaborative;
	}
	
	public void setCollaborative(boolean collaborative){
		this.collaborative = collaborative;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	public String getPicture(){
		return this.picture;
	}
	
	public void setPicture(String picture){
		this.picture = picture;
	}
	
	/**
	 * Get and update the checksum of this playlist.
	 * 
	 * @return The checksum.
	 */
	public long getChecksum(){
		SpotifyChecksum checksum = new SpotifyChecksum(); 
		
		for(Track track : this.tracks){
			checksum.update(track);
		}
		
		this.checksum = checksum.getValue();
		
		return this.checksum;
	}
	
	/**
	 * Set the current checksum of this playlist.
	 * 
	 * @param checksum The current checksum.
	 */
	public void setChecksum(long checksum){
		this.checksum = checksum;
	}
	
	/**
	 * Get the playlists Spotify URI.
	 * 
	 * @return A Spotify URI (e.g. {@code spotify:user:username:playlist:<base62-encoded-id>})
	 */
	public String getURI(){
		return "spotify:user:" + getAuthor() + ":playlist:" + SpotifyURI.toBase62(this.id);
	}
	
	/**
	 * Get the playlists Spotify URI as a HTTP-link.
	 * 
	 * @return A link which redirects to a Spotify URI.
	 */
	public String getLink(){
		return "http://open.spotify.com/user/" + getAuthor() + "/playlist/" + SpotifyURI.toBase62(this.id);
	}
	
	
	public Iterator<Track> iterator(){
		return this.tracks.iterator();
	}
	
	public static Playlist fromResult(String name, String author, Result result){
		Playlist playlist = new Playlist();
		
		playlist.name   = name;
		playlist.author = author;
		
		for(Track track : result.getTracks()){
			playlist.tracks.add(track);
		}
		
		return playlist;
	}
	
	public boolean equals(Object o){
		if(o instanceof Playlist){
			Playlist p = (Playlist)o;
			
			return this.id.equals(p.id);
		}
		
		return false;
	}
	
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
	
	public String toString(){
		return String.format("[Playlist: %s, %s]", this.author, this.name, this.revision);
	}
}
