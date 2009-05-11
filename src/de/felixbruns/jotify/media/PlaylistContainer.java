package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.felixbruns.jotify.util.SpotifyChecksum;
import de.felixbruns.jotify.util.XMLElement;

public class PlaylistContainer implements Iterable<Playlist> {
	private String         author;
	private List<Playlist> playlists;
	private long           revision;
	private long           checksum;
	
	public PlaylistContainer(){
		this.author    = null;
		this.playlists = new ArrayList<Playlist>();
		this.revision  = -1;
		this.checksum  = -1;
	}
	
	public String getAuthor(){
		return this.author;
	}
	
	public void setAuthor(String author){
		this.author = author;
	}
	
	public List<Playlist> getPlaylists(){
		return this.playlists;
	}
	
	public void setPlaylists(List<Playlist> playlists){
		this.playlists = playlists;
	}
	
	public long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	public long getChecksum(){
		SpotifyChecksum checksum = new SpotifyChecksum(); 
		
		for(Playlist playlist : this.playlists){
			checksum.update(playlist);
		}
		
		this.checksum = checksum.getValue();
		
		return this.checksum;
	}
	
	public void setChecksum(long checksum){
		this.checksum = checksum;
	}
	
	public Iterator<Playlist> iterator(){
		return this.playlists.iterator();
	}
	
	public static PlaylistContainer fromXMLElement(XMLElement playlistsElement){
		PlaylistContainer playlists = new PlaylistContainer();
		
		/* Get "change" element. */
		XMLElement changeElement = playlistsElement.getChild("next-change").getChild("change");
		
		/* Get author. */
		playlists.author = changeElement.getChildText("user").trim();
		
		/* Get items (comma separated list). */
		if(changeElement.getChild("ops").hasChild("add")){
			String items = changeElement.getChild("ops").getChild("add").getChildText("items");
			
			for(String id : items.split(",")){
				playlists.playlists.add(new Playlist(id.trim().substring(0, 32), "", playlists.author, false));
			}
		}
		
		/* Get "version" element. */
		XMLElement versionElement = playlistsElement.getChild("next-change").getChild("version");
		
		/* Split version string into parts. */
		String[] parts = versionElement.getText().split(",", 4);
		
		/* Set values. */
		playlists.revision = Long.parseLong(parts[0]);
		playlists.checksum = Long.parseLong(parts[2]);
		
		return playlists;
	}
}
