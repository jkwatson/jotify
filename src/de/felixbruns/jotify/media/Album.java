package de.felixbruns.jotify.media;

import de.felixbruns.jotify.util.Hex;
import de.felixbruns.jotify.util.XMLElement;

public class Album {
	private String id;
	private String name;
	private Artist artist;
	private String cover;
	private float  popularity;
	
	public Album(String id, String name, Artist artist){
		this.id         = id;
		this.name       = name;
		this.artist     = artist;
		this.cover      = null;
		this.popularity = -1.0f;
	}
	
	private Album(){
		this.id         = null;
		this.name       = null;
		this.artist     = null;
		this.cover      = null;
		this.popularity = -1.0f;
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public Artist getArtist(){
		return this.artist;
	}
	
	public String getCover(){
		return this.cover;
	}
	
	public float getPopularity(){
		return this.popularity;
	}
	
	public static Album fromXMLElement(XMLElement albumElement){
		Album album = new Album();
		
		/* Set id. */
		if(albumElement.hasChild("id")){
			album.id = albumElement.getChildText("id");
		}
		
		/* Set name. */
		if(albumElement.hasChild("name")){
			album.name = albumElement.getChildText("name");
		}
		
		/* Set artist. */
		if(albumElement.hasChild("artist-id") && albumElement.hasChild("artist")){
			album.artist = new Artist(
				albumElement.getChildText("artist-id"),
				albumElement.getChildText("artist")
			);
		}
		
		/* Set cover. */
		if(albumElement.hasChild("cover")){
			album.cover = albumElement.getChildText("cover");
		}
		
		/* Set popularity. */
		if(albumElement.hasChild("popularity")){
			album.popularity = Float.parseFloat(albumElement.getChildText("popularity"));
		}
		
		/* TODO: album-type, copyright, discs, ... */
		
		return album;
	}
	
	public String toString(){
		return String.format("[Album: %s by %s]", this.name, this.artist);
	}

	public static Album fromURI(String uri) {
		Album album = new Album();
		album.id = Hex.URIToId(uri);
		return album;
	}
}
