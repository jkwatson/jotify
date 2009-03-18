package de.felixbruns.spotify;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.spotify.util.XMLElement;

public class Result {
	private int          version;
	private String       suggestion;
	private int          totalArtists;
	private int          totalAlbums;
	private int          totalTracks;
	private List<Artist> artists;
	private List<Album>  albums;
	private List<Track>  tracks;
	
	private Result(){
		this.version      = 1;
		this.suggestion   = null;
		this.totalArtists = 0;
		this.totalAlbums  = 0;
		this.totalTracks  = 0;
		this.artists      = new ArrayList<Artist>();
		this.albums       = new ArrayList<Album>();
		this.tracks       = new ArrayList<Track>();
	}
	
	public int getVersion(){
		return this.version;
	}
	
	public String getSuggestion(){
		return this.suggestion;
	}
	
	public int getTotalArtists(){
		return this.totalArtists;
	}
	
	public int getTotalAlbums(){
		return this.totalAlbums;
	}
	
	public int getTotalTracks(){
		return this.totalTracks;
	}
	
	public List<Artist> getArtists(){
		return this.artists;
	}
	
	public List<Album> getAlbums(){
		return this.albums;
	}
	
	public List<Track> getTracks(){
		return this.tracks;
	}
	
	public static Result fromXMLElement(XMLElement resultElement){
		/* Create Result object. */
		Result result = new Result();
		
		/* Set version. */
		if(resultElement.hasChild("version")){
			result.version = Integer.parseInt(resultElement.getChildText("version"));
		}
		
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
	
	public String toString(){
		return String.format("[Result...]");
	}
}
