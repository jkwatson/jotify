package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.SpotifyURI;
import de.felixbruns.jotify.util.XMLElement;

/**
 * Holds information about an artist.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Artist {
	/**
	 * Identifier for this artist (32-character string).
	 */
	private String id;
	
	/**
	 * Name of this artist.
	 */
	private String name;
	
	/**
	 * The identifier for this artists portrait image (32-character string).
	 */
	private String portrait;
	
	/**
	 * Popularity of this artist (from 0.0 to 1.0).
	 */
	private float popularity;
	
	/**
	 * A {@link List} of similar artists.
	 */
	private List<Artist> similarArtists;
	
	/**
	 * Creates an empty {@link Artist} object.
	 */
	private Artist(){
		this.id             = null;
		this.name           = null;
		this.portrait       = null;
		this.popularity     = Float.NaN;
		this.similarArtists = new ArrayList<Artist>();
	}
	
	/**
	 * Creates an {@link Artist} object with the specified {@code id}.
	 * 
	 * @param id Id of the artist.
	 */
	public Artist(String id){
		this(id, null);
	}
	
	/**
	 * Creates an {@link Artist} object with the specified {@code id} and {@code name}.
	 * 
	 * @param id   Id of the artist.
	 * @param name Name of the artist or {@code null}.
	 */
	public Artist(String id, String name){
		/* Check if id string is valid. */
		if(id == null || id.length() != 32 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 32-character hex string.");
		}
		
		/* Set object properties. */
		this.id             = id;
		this.name           = name;
		this.portrait       = null;
		this.popularity     = Float.NaN;
		this.similarArtists = new ArrayList<Artist>();
	}
	
	/**
	 * Get the artists identifier.
	 * 
	 * @return A 32-character identifier.
	 */
	public String getId(){
		return this.id;
	}
	
	/**
	 * Set the artists identifier.
	 * 
	 * @param id A 32-character identifier.
	 */
	public void setId(String id){
		/* Check if id string is valid. */
		if(id == null || id.length() != 32 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 32-character hex string.");
		}
		
		this.id = id;
	}
	
	/**
	 * Get the artists Spotify URI.
	 * 
	 * @return A Spotify URI (e.g. {@code spotify:artist:<base62-encoded-id>})
	 */
	public String getURI(){
		return "spotify:artist:" + SpotifyURI.toURI(this.id);
	}
	
	/**
	 * Get the artists Spotify URI as a HTTP-link.
	 * 
	 * @return A link which redirects to a Spotify URI.
	 */
	public String getLink(){
		return "http://open.spotify.com/artist/" + SpotifyURI.toURI(this.id);
	}
	
	/**
	 * Get the artists name.
	 * 
	 * @return The name of this artist.
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Set the artists name.
	 * 
	 * @param name The desired name of this artist.
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Get the artists portrait image identifier.
	 * 
	 * @return A 32-character image identifier.
	 */
	public String getPortrait(){
		return this.portrait;
	}
	
	/**
	 * Set the artists portrait image identifier.
	 * 
	 * @param portrait A 32-character image identifier.
	 */
	public void setPortrait(String portrait){
		/* Check if portrait id string is valid. */
		if(portrait == null || portrait.length() != 32 || !Hex.isHex(portrait)){
			throw new IllegalArgumentException("Expecting a 32-character hex string.");
		}
		
		this.portrait = portrait;
	}
	
	/**
	 * Get the artists popularity.
	 * 
	 * @return A decimal value between 0.0 and 1.0 or {@link Float.NAN} if not available.
	 */
	public float getPopularity(){
		return this.popularity;
	}
	
	/**
	 * Set the artists popularity.
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
	 * Get similar artists for this artist.
	 * 
	 * @return A {@link List} of {@link Artist} objects.
	 */
	public List<Artist> getSimilarArtists(){
		return this.similarArtists;
	}
	
	/**
	 * Set similar artists for this artist.
	 * 
	 * @param similarArtists A {@link List} of {@link Artist} objects.
	 */
	public void setSimilarArtists(List<Artist> similarArtists){
		this.similarArtists = similarArtists;
	}
	
	/**
	 * Create an {@link Artist} object from an {@link XMLElement} holding artist information.
	 * 
	 * @param artistElement An {@link XMLElement} holding artist information.
	 * 
	 * @return An {@link Artist} object.
	 */
	public static Artist fromXMLElement(XMLElement artistElement){
		/* Create an empty artist object. */
		Artist artist = new Artist();
		
		/* Set identifier. */
		if(artistElement.hasChild("id")){
			artist.id = artistElement.getChildText("id");
		}
		
		/* Set name. */
		if(artistElement.hasChild("name")){
			artist.name = artistElement.getChildText("name");
		}
		
		/* Set portrait. */
		if(artistElement.hasChild("portrait") && artistElement.getChild("portrait").hasChild("id")){
			String value = artistElement.getChild("portrait").getChildText("id");
			
			if(!value.isEmpty()){
				artist.portrait = value;
			}
		}
		
		/* Set popularity. */
		if(artistElement.hasChild("popularity")){
			artist.popularity = Float.parseFloat(artistElement.getChildText("popularity"));
		}
		
		/* Set similar artists. */
		if(artistElement.hasChild("similar-artists")){
			for(XMLElement similarArtistElement : artistElement.getChild("similar-artists").getChildren()){
				artist.similarArtists.add(Artist.fromXMLElement(similarArtistElement));
			}
		}
		
		/* TODO: bios, genres, years-active, albums, ... */
		
		return artist;
	}
	
	/**
	 * Determines if an object is equal to this {@link Artist} object.
	 * If both objects are {@link Artist} objects, it will compare their identifiers.
	 * 
	 * @param o Another object to compare.
	 * 
	 * @return true of the objects are equal, false otherwise.
	 */
	public boolean equals(Object o){
		if(o instanceof Artist){
			Artist a = (Artist)o;
			
			return this.id.equals(a.id);
		}
		
		return false;
	}
	
	/**
	 * Return the hash code of this {@link Artist} object. This will give the value returned
	 * by the {@code hashCode} method of the identifier string.
	 * 
	 * @return The {@link Artist} objects hash code.
	 */
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
}
