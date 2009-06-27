package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.XMLElement;

public class Result {
	private String       query;
	private String       suggestion;
	private int          totalArtists;
	private int          totalAlbums;
	private int          totalTracks;
	private List<Artist> artists;
	private List<Album>  albums;
	private List<Track>  tracks;
	
	public Result(){
		this.query        = null;
		this.suggestion   = null;
		this.totalArtists = 0;
		this.totalAlbums  = 0;
		this.totalTracks  = 0;
		this.artists      = new ArrayList<Artist>();
		this.albums       = new ArrayList<Album>();
		this.tracks       = new ArrayList<Track>();
	}
	
	public String getQuery(){
		return this.query;
	}
	
	public void setQuery(String query){
		this.query = query;
	}
	
	public String getSuggestion(){
		return this.suggestion;
	}
	
	public void setSuggestion(String suggestion){
		this.suggestion = suggestion;
	}
	
	public int getTotalArtists(){
		return this.totalArtists;
	}
	
	public void setTotalArtists(int totalArtists){
		this.totalArtists = totalArtists;
	}
	
	public int getTotalAlbums(){
		return this.totalAlbums;
	}
	
	public void setTotalAlbums(int totalAlbums){
		this.totalAlbums = totalAlbums;
	}
	
	public int getTotalTracks(){
		return this.totalTracks;
	}
	
	public void setTotalTracks(int totalTracks){
		this.totalTracks = totalTracks;
	}
	
	public List<Artist> getArtists(){
		return this.artists;
	}
	
	public void setArtists(List<Artist> artists){
		this.artists = artists;
	}
	
	public void addArtist(Artist artist){
		this.artists.add(artist);
	}
	
	public List<Album> getAlbums(){
		return this.albums;
	}
	
	public void setAlbums(List<Album> albums){
		this.albums = albums;
	}
	
	public void addAlbum(Album album){
		this.albums.add(album);
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
	
	public static Result fromXMLElement(XMLElement resultElement){
		return fromXMLElement("", resultElement);
	}
	
	public static Result fromXMLElement(String query, XMLElement resultElement){
		/* Create Result object. */
		Result result = new Result();
		
		/* Set query. */
		result.query = query;
		
		/* Set suggestion. */
		if(resultElement.hasChild("did-you-mean")){
			result.suggestion = resultElement.getChildText("did-you-mean");
		}
		
		/* Set result quantities.*/
		if(resultElement.hasChild("total-artists") &&
			resultElement.hasChild("total-albums") &&
			resultElement.hasChild("total-tracks")){
			result.totalArtists = Integer.parseInt(resultElement.getChildText("total-artists"));
			result.totalAlbums  = Integer.parseInt(resultElement.getChildText("total-albums"));
			result.totalTracks  = Integer.parseInt(resultElement.getChildText("total-tracks"));
		}
		
		/* Get artists. */
		if(resultElement.hasChild("artists")){
			for(XMLElement artistElement : resultElement.getChild("artists").getChildren()){
				result.artists.add(Artist.fromXMLElement(artistElement));
			}
		}
		
		/* Get albums. */
		if(resultElement.hasChild("albums")){
			for(XMLElement albumElement : resultElement.getChild("albums").getChildren()){
				result.albums.add(Album.fromXMLElement(albumElement));
			}
		}
		
		/* Get tracks. */
		if(resultElement.hasChild("tracks")){
			for(XMLElement trackElement : resultElement.getChild("tracks").getChildren()){
				result.tracks.add(Track.fromXMLElement(trackElement));
			}
		}
		
		/* Return result. */
		return result;
	}
	
	public boolean equals(Object o){
		if(o instanceof Result){
			return this.query.equals(((Result)o).query);			
		}
		
		return false;
	}
	
	public int hashCode(){
		return (this.query != null) ? this.query.hashCode() : 0;
	}
}
