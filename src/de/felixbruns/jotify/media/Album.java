package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.XMLElement;

public class Album {
	private String      id;
	private String      name;
	private Artist      artist;
	private String      cover;
	private int         year;
	private float       popularity;
	private List<Track> tracks;
	
	private Album(){
		this(null, null, null);
	}
	
	public Album(String id, String name, Artist artist){
		this.id         = id;
		this.name       = name;
		this.artist     = artist;
		this.cover      = null;
		this.year       = -1;
		this.popularity = Float.NaN;
		this.tracks     = new ArrayList<Track>();
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
	
	public Artist getArtist(){
		return this.artist;
	}
	
	public void setArtist(Artist artist){
		this.artist = artist;
	}
	
	public String getCover(){
		return this.cover;
	}
	
	public void setCover(String cover){
		this.cover = cover;
	}
	
	public int getYear(){
		return this.year;
	}
	
	public void setYear(int year){
		this.year = year;
	}
	
	public float getPopularity(){
		return this.popularity;
	}
	
	public void setPopularity(float popularity){
		this.popularity = popularity;
	}
	
	public List<Track> getTracks(){
		return this.tracks;
	}
	
	public void setTracks(List<Track> tracks){
		this.tracks = tracks;
	}
	
	public void addTrack(Track track){
		this.tracks.add(track);
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
		if(albumElement.hasChild("artist-id") && (albumElement.hasChild("artist") || albumElement.hasChild("artist-name"))){
			album.artist = new Artist(
				albumElement.getChildText("artist-id"),
				albumElement.hasChild("artist")?albumElement.getChildText("artist"):albumElement.getChildText("artist-name")
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
			for(XMLElement discElement : albumElement.getChild("discs").getChildren("disc")){
				for(XMLElement trackElement : discElement.getChildren("track")){
					Track track = Track.fromXMLElement(trackElement);
					
					track.setAlbum(album);
					
					album.tracks.add(track);
				}
			}
		}		
		
		/* TODO: album-type, copyright, discs, ... */
		
		return album;
	}
	
	public boolean equals(Object o){
		if(o instanceof Album){
			Album a = (Album)o;
			
			return this.id.equals(a.id);
		}
		
		return false;
	}
	
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
}
