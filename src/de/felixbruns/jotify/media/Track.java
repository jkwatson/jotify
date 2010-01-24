package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.SpotifyURI;

/**
 * Holds information about a track.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Track extends Media {
	/**
	 * Title of this track.
	 */
	private String title;
	
	/**
	 * {@link Artist} of this track.
	 */
	private Artist artist;
	
	/**
	 * {@link Album} this track belongs to.
	 */
	private Album album;
	
	/**
	 * Release year of this track.
	 */
	private int year;
	
	/**
	 * Track number on a certain disk.
	 */
	private int trackNumber;
	
	/**
	 * Length of this track in seconds.
	 */
	private int length;
	
	/**
	 * Files available for this track.
	 */
	private List<File> files;
	
	/**
	 * The identifier for this tracks cover image (32-character string).
	 */
	private String cover;
	
	/**
	 * Similar tracks of this track.
	 */
	private List<Track> similarTracks;
	
	/**
	 * Creates an empty {@link Track} object.
	 */
	public Track(){
		this.title         = null;
		this.artist        = null;
		this.album         = null;
		this.year          = -1;
		this.trackNumber   = -1;
		this.length        = -1;
		this.files         = new ArrayList<File>();
		this.cover         = null;
		this.similarTracks = new ArrayList<Track>();
	}
	
	/**
	 * Creates a {@link Track} object with the specified {@code id}.
	 * 
	 * @param id Id of the track.
	 */
	public Track(String id){
		this(id, null, null, null);
	}
	
	/**
	 * Creates a {@link Track} object with the specified {@code id},
	 * {@code title},{@code artist} and {@code album}.
	 * 
	 * @param id     Id of the track.
	 * @param title  Title of the track.
	 * @param artist Artist of the track.
	 * @param album  Album of the track.
	 */
	public Track(String id, String title, Artist artist, Album album){
		super(id);
		
		/* Set object properties. */
		this.title         = title;
		this.artist        = artist;
		this.album         = album;
		this.year          = -1;
		this.trackNumber   = -1;
		this.length        = -1;
		this.files         = new ArrayList<File>();
		this.cover         = null;
		this.similarTracks = new ArrayList<Track>();
	}
	
	/**
	 * Get the tracks Spotify URI.
	 * 
	 * @return A Spotify URI (e.g. {@code spotify:track:<base62-encoded-id>})
	 */
	public String getURI(){
		return "spotify:track:" + SpotifyURI.toBase62(this.id);
	}
	
	/**
	 * Get the tracks Spotify URI as a HTTP-link.
	 * 
	 * @return A link which redirects to a Spotify URI.
	 */
	public String getLink(){
		return "http://open.spotify.com/track/" + SpotifyURI.toBase62(this.id);
	}
	
	/**
	 * Get the tracks title.
	 * 
	 * @return The title of this track.
	 */
	public String getTitle(){
		return this.title;
	}
	
	/**
	 * Set the tracks title.
	 * 
	 * @param name The desired name of this track.
	 */
	public void setTitle(String title){
		this.title = title;
	}
	
	/**
	 * Get the tracks artist.
	 * 
	 * @return An {@link Artist} object.
	 */
	public Artist getArtist(){
		return this.artist;
	}
	
	/**
	 * Set the tracks artist.
	 * 
	 * @param artist The desired {@link Artist} of this track.
	 */
	public void setArtist(Artist artist){
		this.artist = artist;
	}
	
	/**
	 * Get the tracks album.
	 * 
	 * @return An {@link Album} object.
	 */
	public Album getAlbum(){
		return this.album;
	}
	
	/**
	 * Set the tracks album.
	 * 
	 * @param album The desired {@link Album} of this track.
	 */
	public void setAlbum(Album album){
		this.album = album;
	}
	
	/**
	 * Get the tracks release year.
	 * 
	 * @return An integer denoting the release year or -1 if not available.
	 */
	public int getYear(){
		return this.year;
	}
	
	/**
	 * Set the tracks release year.
	 * 
	 * @param year A positive integer denoting the release year.
	 */
	public void setYear(int year){
		/* Check if year is valid. Years B.C. are not supported :-P */
		if(year < 0){
			throw new IllegalArgumentException("Expecting a positive year.");
		}
		
		this.year = year;
	}
	
	/**
	 * Get the tracks number on a certain disk.
	 * 
	 * @return An integer denoting the track number or -1 if not available.
	 */
	public int getTrackNumber(){
		return this.trackNumber;
	}
	
	/**
	 * Set the tracks number on a certain disk.
	 * 
	 * @param trackNumber A positive integer greater than zero.
	 */
	public void setTrackNumber(int trackNumber){
		/* Check if track number is valid. */
		if(trackNumber <= 0){
			throw new IllegalArgumentException("Expecting a track number greater than zero.");
		}
		
		this.trackNumber = trackNumber;
	}
	
	/**
	 * Get the tracks length in milliseconds.
	 * 
	 * @return An integer representing the length in milliseconds or -1 if not available.
	 */
	public int getLength(){
		return this.length;
	}
	
	/**
	 * Set the tracks length in milliseconds.
	 * 
	 * @return length A positive integer greater than zero representing the length in milliseconds.
	 */
	public void setLength(int length){
		/* Check if length is valid. */
		if(length <= 0){
			throw new IllegalArgumentException("Expecting a length greater than zero.");
		}
		
		this.length = length;
	}
	
	/**
	 * Get a list of files of this track.
	 * 
	 * @return A {@link List} of {@link File} objects.
	 */
	public List<File> getFiles(){
		return this.files;
	}
	
	/**
	 * Set the list of {@link File} objects for this track.
	 * 
	 * @param files A {@link List} of {@link File} objects.
	 */
	public void setFiles(List<File> files){
		this.files = files;
	}
	
	/**
	 * Add a {@link File} to the list of files.
	 * 
	 * @param id The {@link File} to add.
	 */
	public void addFile(File file){
		this.files.add(file);
	}
	
	/**
	 * Get the tracks cover image identifier.
	 * 
	 * @return A 32-character image identifier.
	 */
	public String getCover(){
		return this.cover;
	}
	
	/**
	 * Set the tracks cover image identifier.
	 * 
	 * @param cover A 32-character image identifier.
	 */
	public void setCover(String cover){
		this.cover = cover;
	}
	
	/**
	 * Get similar tracks for this track.
	 * 
	 * @return A {@link List} of {@link Track} objects.
	 */
	public List<Track> getSimilarTracks(){
		return this.similarTracks;
	}
	
	/**
	 * Set similar tracks for this track.
	 * 
	 * @param similarTracks A {@link List} of {@link Track} objects.
	 */
	public void setSimilarTracks(List<Track> similarTracks){
		this.similarTracks = similarTracks;
	}
	
	/**
	 * Determines if an object is equal to this {@link Track} object.
	 * If both objects are {@link Track} objects, it will compare their identifiers.
	 * 
	 * @param o Another object to compare.
	 * 
	 * @return true of the objects are equal, false otherwise.
	 */
	public boolean equals(Object o){
		if(o instanceof Track){
			Track t = (Track)o;
			
			if(this.id.equals(t.id)){
				return true;
			}
			
			for(String id : this.redirects){
				if(id.equals(t.id)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Return the hash code of this {@link Track} object. This will give the value returned
	 * by the {@code hashCode} method of the identifier string.
	 * 
	 * @return The {@link Track} objects hash code.
	 */
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
	
	public String toString(){
		return String.format("[Track: %s, %s, %s]", this.artist, this.album, this.title);
	}
}
