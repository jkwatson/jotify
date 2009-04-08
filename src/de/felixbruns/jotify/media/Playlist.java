package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.util.XMLElement;

public class Playlist {
	/* TODO: version! */
	private String      id;
	private String      name;
	private String      author;
	private List<Track> tracks;
	
	public Playlist(String id, String name, String author){
		this.id     = id;
		this.name   = name;
		this.author = author;
		this.tracks = new ArrayList<Track>();
	}
	
	public Playlist(){
		this.id     = null;
		this.name   = null;
		this.author = null;
		this.tracks = new ArrayList<Track>();
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getAuthor(){
		return this.author;
	}
	
	public List<Track> getTracks(){
		return this.tracks;
	}
	
	public void setTracks(List<Track> tracks){
		this.tracks = tracks;
	}
	
	public boolean equals(Object o){
		if(o instanceof Playlist){
			/* TODO: version! */
			return this.id.equals(((Playlist)o).id);
		}
		
		return false;
	}
	
	public static List<Playlist> listFromXMLElement(XMLElement playlistsElement){
		List<Playlist> playlists = new ArrayList<Playlist>();
		
		/* Get "change" element. */
		playlistsElement = playlistsElement.getChild("next-change").getChild("change");
		
		/* Get author. */
		String author = playlistsElement.getChildText("user");
		
		/* Get items (comma separated list). */
		if(playlistsElement.getChild("ops").hasChild("add")){
			String items = playlistsElement.getChild("ops").getChild("add").getChildText("items");
			
			for(String id : items.split(",")){
				playlists.add(new Playlist(id.trim(), "", author.trim()));
			}
		}
		
		return playlists;
	}
	
	public static Playlist fromXMLElement(XMLElement playlistElement, String id){
		Playlist playlist = new Playlist();
		
		/* Get "change" element. */
		playlistElement = playlistElement.getChild("next-change").getChild("change");
		
		/* Set id. */
		playlist.id = id;
		
		/* Set author. */
		playlist.author = playlistElement.getChildText("user");
		
		/* Set name. */
		playlist.name = playlistElement.getChild("ops").getChildText("name");
		
		/* Get items (comma separated list). */
		String items = playlistElement.getChild("ops").getChild("add").getChildText("items");
		
		for(String trackId : items.split(",")){
			playlist.tracks.add(new Track(trackId.trim(), "", null, null));
		}
		
		return playlist;
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
}
