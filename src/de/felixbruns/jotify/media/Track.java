package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.SpotifyURI;
import de.felixbruns.jotify.util.XMLElement;

public class Track {
	private String       id;
	private String       title;
	private Artist       artist;
	private Album        album;
	private int          year;
	private int          trackNumber;
	private int          length;
	private List<String> files;
	private String       cover;
	private float        popularity;
	
	private Track(){
		this(null, null, null, null);
	}
	
	public Track(String id, String title, Artist artist, Album album){
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
	
	public String getId(){
		return this.id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getURI(){
		return "spotify:track:" + SpotifyURI.toURI(this.id);
	}
	
	public String getLink(){
		return "http://open.spotify.com/track/" + SpotifyURI.toURI(this.id);
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public Artist getArtist(){
		return this.artist;
	}
	
	public void setArtist(Artist artist){
		this.artist = artist;
	}
	
	public Album getAlbum(){
		return this.album;
	}
	
	public void setAlbum(Album album){
		this.album = album;
	}
	
	public int getYear(){
		return this.year;
	}
	
	public void setYear(int year){
		this.year = year;
	}
	
	public int getTrackNumber(){
		return this.trackNumber;
	}
	
	public void setTrackNumber(int trackNumber){
		this.trackNumber = trackNumber;
	}
	
	public int getLength(){
		return this.length;
	}
	
	public void setLength(int length){
		this.length = length;
	}
	
	public List<String> getFiles(){
		return this.files;
	}
	
	public void setFiles(List<String> files){
		this.files = files;
	}
	
	public void addFile(String id){
		this.files.add(id);
	}
	
	public String getCover(){
		return this.cover;
	}
	
	public void setCover(String cover){
		this.cover = cover;
	}
	
	public float getPopularity(){
		return this.popularity;
	}
	
	public void setPopularity(float popularity){
		this.popularity = popularity;
	}
	
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
	
	public boolean equals(Object o){
		if(o instanceof Track){
			Track t = (Track)o;
			
			return this.id.equals(t.id);
		}
		
		return false;
	}
	
	public int hashCode(){
		return (this.id != null) ? this.id.hashCode() : 0;
	}
}
