package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.felixbruns.jotify.media.Link.InvalidSpotifyURIException;
import de.felixbruns.jotify.util.Hex;

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
		this.id            = null;
		this.name          = null;
		this.author        = null;
		this.tracks        = new ArrayList<Track>();
		this.revision      = -1;
		this.checksum      = -1;
		this.collaborative = false;
		this.description   = null;
		this.picture       = null;
	}
	
	public Playlist(String id){
		this(id, null, null, false);
	}
	
	public Playlist(String id, String name, String author, boolean collaborative){
		/* Check if id is a 32-character hex string. */
		if(id.length() == 32 && Hex.isHex(id)){
			this.id = id;
		}
		/* Otherwise try to parse it as a Spotify URI. */
		else{
			try{
				this.id = Link.create(id).getId();
			}
			catch(InvalidSpotifyURIException e){
				throw new IllegalArgumentException(
					"Given id is neither a 32-character" +
					"hex string nor a valid Spotify URI.", e
				);
			}
		}
		
		/* Set other playlist properties. */
		this.name          = name;
		this.author        = author;
		this.tracks        = new ArrayList<Track>();
		this.revision      = -1;
		this.checksum      = -1;
		this.collaborative = collaborative;
		this.description   = null;
		this.picture       = null;
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
		Checksum checksum = new Checksum(); 
		
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
	 * Create a link from this playlist.
	 * 
	 * @return A {@link Link} object which can then
	 * 		   be used to retreive the Spotify URI.
	 */
	public Link getLink(){
		return Link.create(this);
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
		return String.format("[Playlist: %s, %s, %d]", this.author, this.name, this.revision);
	}
}
