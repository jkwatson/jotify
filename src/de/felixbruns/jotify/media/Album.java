package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.SpotifyURI;
import de.felixbruns.jotify.util.XMLElement;

/**
 * Holds information about an album.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Album extends Media {
	/**
	 * Name of this album.
	 */
	private String name;
	
	/**
	 * Artist of this album.
	 */
	private Artist artist;
	
	/**
	 * The identifier for this albums cover image (32-character string).
	 */
	private String cover;
	
	/**
	 * The type of this album (compilation, album, single).
	 */
	private String type;
	
	/**
	 * The review of this album.
	 */
	private String review;
	
	/**
	 * Release year of this album.
	 */
	private int year;
	
	/**
	 * A {@link List} of discs of this album.
	 * 
	 * @see Disc
	 */
	private List<Disc> discs;
	
	/**
	 * Similar albums of this album.
	 */
	private List<Album> similarAlbums;
	
	/**
	 * Creates an empty {@link Album} object.
	 */
	public Album(){
		this.name          = null;
		this.artist        = null;
		this.cover         = null;
		this.type          = null;
		this.review        = null;
		this.year          = -1;
		this.discs         = new ArrayList<Disc>();
		this.similarAlbums = new ArrayList<Album>();
	}
	
	/**
	 * Creates an {@link Album} object with the specified {@code id}.
	 * 
	 * @param id Id of the album.
	 */
	public Album(String id){
		this(id, null, null);
	}
	
	/**
	 * Creates an {@link Album} object with the specified {@code id}, {@code name} and {@code artist}.
	 * 
	 * @param id     Id of the album.
	 * @param name   Name of the album.
	 * @param artist Artist of the album.
	 */
	public Album(String id, String name, Artist artist){
		super(id);
		
		/* Set object properties. */
		this.name          = name;
		this.artist        = artist;
		this.cover         = null;
		this.type          = null;
		this.review        = null;
		this.year          = -1;
		this.discs         = new ArrayList<Disc>();
		this.similarAlbums = new ArrayList<Album>();
	}
	
	/**
	 * Get the albums Spotify URI.
	 * 
	 * @return A Spotify URI (e.g. {@code spotify:album:<base62-encoded-id>})
	 */
	public String getURI(){
		return "spotify:album:" + SpotifyURI.toURI(this.id);
	}
	
	/**
	 * Get the albums Spotify URI as a HTTP-link.
	 * 
	 * @return A link which redirects to a Spotify URI.
	 */
	public String getLink(){
		return "http://open.spotify.com/album/" + SpotifyURI.toURI(this.id);
	}
	
	/**
	 * Get the albums name.
	 * 
	 * @return The name of this album.
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Set the albums name.
	 * 
	 * @param name The desired name of this album.
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Get the albums artist.
	 * 
	 * @return An {@link Artist} object.
	 */
	public Artist getArtist(){
		return this.artist;
	}
	
	/**
	 * Set the albums artist.
	 * 
	 * @param artist The desired {@link Artist} of this album.
	 */
	public void setArtist(Artist artist){
		this.artist = artist;
	}
	
	/**
	 * Get the albums cover image identifier.
	 * 
	 * @return A 32-character image identifier.
	 */
	public String getCover(){
		return this.cover;
	}
	
	/**
	 * Set the albums cover image identifier.
	 * 
	 * @param cover A 40-character image identifier.
	 */
	public void setCover(String cover){
		/* Check if portrait id string is valid. */
		if(cover == null || cover.length() != 40 || !Hex.isHex(cover)){
			throw new IllegalArgumentException("Expecting a 40-character hex string.");
		}
		
		this.cover = cover;
	}
	
	/**
	 * Get the albums type.
	 * 
	 * @return One of "compilation", "album" or "single".
	 */
	public String getType(){
		return this.type;
	}
	
	/**
	 * Set the albums type.
	 * 
	 * @param type One of "compilation", "album" or "single".
	 */
	public void setType(String type){
		this.type = type;
	}
	
	/**
	 * Get the albums review.
	 * 
	 * @return A review of this album.
	 */
	public String getReview(){
		return this.review;
	}
	
	/**
	 * Set the albums review.
	 * 
	 * @param review A review of this album.
	 */
	public void setReview(String review){
		this.review = review;
	}
	
	/**
	 * Get the albums release year.
	 * 
	 * @return An integer denoting the release year or -1 if not available.
	 */
	public int getYear(){
		return this.year;
	}
	
	/**
	 * Set the albums release year.
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
	 * Get discs of this album.
	 * 
	 * @return A {@link List} of {@link Disc} objects.
	 */
	public List<Disc> getDiscs(){
		return this.discs;
	}
	
	/**
	 * Set discs of this album.
	 * 
	 * @param discs A {@link List} of {@link Disc} objects.
	 */
	public void setDiscs(List<Disc> discs){
		this.discs = discs;
	}
	
	/**
	 * Set discs for this album.
	 * 
	 * @param discs A {@link List} of {@link Disc} objects.
	 */
	public void setTracks(List<Disc> discs){
		this.discs = discs;
	}
	
	/**
	 * Get tracks of this album. This automatically merges
	 * tracks from this albums discs.
	 * 
	 * @return A {@link List} of {@link Track} objects.
	 */
	public List<Track> getTracks(){
		List<Track> tracks = new ArrayList<Track>();
		
		for(Disc disc : this.discs){
			tracks.addAll(disc.getTracks());
		}
		
		return tracks;
	}
	
	/**
	 * Get similar albums for this album.
	 * 
	 * @return A {@link List} of {@link Album} objects.
	 */
	public List<Album> getSimilarAlbums(){
		return this.similarAlbums;
	}
	
	/**
	 * Set similar albums for this album.
	 * 
	 * @param similarAlbums A {@link List} of {@link Album} objects.
	 */
	public void setSimilarAlbums(List<Album> similarAlbums){
		this.similarAlbums = similarAlbums;
	}
	
	/**
	 * Create an {@link Album} object from an {@link XMLElement} holding album information.
	 * 
	 * @param albumElement An {@link XMLElement} holding album information.
	 * 
	 * @return An {@link Album} object.
	 */
	public static Album fromXMLElement(XMLElement albumElement){
		/* Create an empty album object. */
		Album album = new Album();
		
		/* Set identifier. */
		if(albumElement.hasChild("id")){
			album.id = albumElement.getChildText("id");
		}
		
		/* Set name. */
		if(albumElement.hasChild("name")){
			album.name = albumElement.getChildText("name");
		}
		
		/* Set artist. */
		if(albumElement.hasChild("artist-id") && (albumElement.hasChild("artist") || albumElement.hasChild("artist-name"))){
			album.artist = new Artist(
				albumElement.getChildText("artist-id"),
				albumElement.hasChild("artist")?
					albumElement.getChildText("artist"):
					albumElement.getChildText("artist-name")
			);
		}
		
		/* Set cover. */
		if(albumElement.hasChild("cover")){
			String value = albumElement.getChildText("cover");
			
			if(!value.isEmpty()){
				album.cover = value;
			}
		}
		
		/* Set year. */
		if(albumElement.hasChild("year")){
			album.year = Integer.parseInt(albumElement.getChildText("year"));
		}
		
		/* Set popularity. */
		if(albumElement.hasChild("popularity")){
			album.popularity = Float.parseFloat(albumElement.getChildText("popularity"));
		}
		
		/* Set tracks. */
		if(albumElement.hasChild("discs")){
			/* Loop over discs. */
			for(XMLElement discElement : albumElement.getChild("discs").getChildren("disc")){
				Disc disc = new Disc(
					Integer.parseInt(discElement.getChildText("disc-number")),
					discElement.getChildText("name")
				);
				
				/* Loop over tracks of current disc. */
				for(XMLElement trackElement : discElement.getChildren("track")){
					/* Parse track element. */
					Track track = Track.fromXMLElement(trackElement);
					
					/* Also add artist and album information to track. */
					track.setArtist(album.artist);
					track.setAlbum(album);
					track.setCover(album.cover);
					
					/* Add track to list of tracks. */
					disc.getTracks().add(track);
				}
				
				album.discs.add(disc);
			}
		}		
		
		/* TODO: album-type, copyright, discs, ... */
		
		return album;
	}
	
	/**
	 * Determines if an object is equal to this {@link Album} object.
	 * If both objects are {@link Album} objects, it will compare their identifiers.
	 * 
	 * @param o Another object to compare.
	 * 
	 * @return true of the objects are equal, false otherwise.
	 */
	public boolean equals(Object o){
		if(o instanceof Album){
			Album a = (Album)o;
			
			return this.id.equals(a.id);
		}
		
		return false;
	}
	
	/**
	 * Return the hash code of this {@link Album} object. This will give the value returned
	 * by the {@code hashCode} method of the identifier string.
	 * 
	 * @return The {@link Album} objects hash code.
	 */
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
	
	public String toString(){
		return String.format("[Album: %s, %s]", this.artist, this.name);
	}
}
