package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.SpotifyURI;
import de.felixbruns.jotify.util.XMLElement;

/**
 * Holds information about a track.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Track {
	/**
	 * Identifier for this track (32-character string).
	 */
	private String id;
	
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
	 * File identifiers for this track (40-character strings).
	 */
	private List<String> files;
	
	/**
	 * The identifier for this tracks cover image (32-character string).
	 */
	private String cover;
	
	/**
	 * Popularity of this track (from 0.0 to 1.0).
	 */
	private float popularity;
	
	/**
	 * Creates an empty {@link Track} object.
	 */
	private Track(){
		this.id          = null;
		this.title       = null;
		this.artist      = null;
		this.album       = null;
		this.year        = -1;
		this.trackNumber = -1;
		this.length      = -1;
		this.files       = new ArrayList<String>();
		this.cover       = null;
		this.popularity  = Float.NaN;
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
		/* Check if id string is valid. */
		if(id == null || id.length() != 32 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 32-character hex string.");
		}
		
		/* Set object properties. */
		this.id          = id;
		this.title       = title;
		this.artist      = artist;
		this.album       = album;
		this.year        = -1;
		this.trackNumber = -1;
		this.length      = -1;
		this.files       = new ArrayList<String>();
		this.cover       = null;
		this.popularity  = Float.NaN;
	}
	
	/**
	 * Get the tracks identifier.
	 * 
	 * @return A 32-character identifier.
	 */
	public String getId(){
		return this.id;
	}
	
	/**
	 * Set the tracks identifier.
	 * 
	 * @param id A 32-character identifier.
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * Get the tracks Spotify URI.
	 * 
	 * @return A Spotify URI (e.g. {@code spotify:track:<base62-encoded-id>})
	 */
	public String getURI(){
		return "spotify:track:" + SpotifyURI.toURI(this.id);
	}
	
	/**
	 * Get the tracks Spotify URI as a HTTP-link.
	 * 
	 * @return A link which redirects to a Spotify URI.
	 */
	public String getLink(){
		return "http://open.spotify.com/track/" + SpotifyURI.toURI(this.id);
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
	 * Get the tracks length in seconds.
	 * 
	 * @return An integer representing the length in seconds or -1 if not available.
	 */
	public int getLength(){
		return this.length;
	}
	
	/**
	 * Set the tracks length in seconds.
	 * 
	 * @return length A positive integer greater than zero representing the length in seconds.
	 */
	public void setLength(int length){
		/* Check if length is valid. */
		if(length <= 0){
			throw new IllegalArgumentException("Expecting a length greater than zero.");
		}
		
		this.length = length;
	}
	
	/**
	 * Get a list of file ids of this track.
	 * 
	 * @return A {@link List} of file ids.
	 */
	public List<String> getFiles(){
		return this.files;
	}
	
	/**
	 * Set the file id list of this track.
	 * 
	 * @param files A {@link List} of file ids.
	 */
	public void setFiles(List<String> files){
		this.files = files;
	}
	
	/**
	 * Add a file id to the list of file ids.
	 * 
	 * @param id The file id to add.
	 */
	public void addFile(String id){
		this.files.add(id);
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
	 * Get the tracks popularity.
	 * 
	 * @return A decimal value between 0.0 and 1.0 or {@link Float.NAN} if not available.
	 */
	public float getPopularity(){
		return this.popularity;
	}
	
	/**
	 * Set the tracks popularity.
	 * 
	 * @param popularity A decimal value between 0.0 and 1.0 or {@link Float.NAN}.
	 */
	public void setPopularity(float popularity){
		/* Check if popularity value is valid. */
		if(popularity != Float.NaN || popularity < 0.0 || popularity > 1.0){
			throw new IllegalArgumentException("Expecting a value from 0.0 to 1.0 or Float.NAN.");
		}
		
		this.popularity = popularity;
	}
	
	/**
	 * Create a {@link Track} object from an {@link XMLElement} holding track information.
	 * 
	 * @param trackElement An {@link XMLElement} holding track information.
	 * 
	 * @return A {@link Track} object.
	 */
	public static Track fromXMLElement(XMLElement trackElement){
		Track track = new Track();
		
		/* Set id. */
		if(trackElement.hasChild("id")){
			track.id = trackElement.getChildText("id");
		}
		
		/* Set title. */
		if(trackElement.hasChild("title")){
			track.title = trackElement.getChildText("title");
		}
		
		/* Set artist. */
		if(trackElement.hasChild("artist-id") && trackElement.hasChild("artist")){
			track.artist = new Artist(
				trackElement.getChildText("artist-id"),
				trackElement.getChildText("artist")
			);
		}
		
		/* Set album. */
		if(trackElement.hasChild("album-id") && trackElement.hasChild("album")){
			track.album = new Album(
				trackElement.getChildText("album-id"),
				trackElement.getChildText("album"),
				track.artist
			);
		}
		
		/* Set year. */
		if(trackElement.hasChild("year")){
			try{
				track.year = Integer.parseInt(trackElement.getChildText("year"));
			}
			catch(NumberFormatException e){
				track.year = -1;
			}
		}
		
		/* Set track number. */
		if(trackElement.hasChild("track-number")){
			track.trackNumber = Integer.parseInt(trackElement.getChildText("track-number"));
		}
		
		/* Set length. */
		if(trackElement.hasChild("length")){
			track.length = Integer.parseInt(trackElement.getChildText("length"));
		}
		
		/* Set files. */
		if(trackElement.hasChild("files")){
			for(XMLElement fileElement : trackElement.getChild("files").getChildren()){
				track.files.add(fileElement.getAttribute("id"));
			}
		}
		
		/* Set cover. */
		if(trackElement.hasChild("cover")){
			String value = trackElement.getChildText("cover");
			
			if(!value.isEmpty()){
				track.cover = value;
			}
		}
		
		/* Set popularity. */
		if(trackElement.hasChild("popularity")){
			track.popularity = Float.parseFloat(trackElement.getChildText("popularity"));
		}
		
		return track;
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
			
			return this.id.equals(t.id);
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
}
