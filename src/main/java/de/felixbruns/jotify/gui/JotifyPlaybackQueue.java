package de.felixbruns.jotify.gui;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.felixbruns.jotify.media.Track;

public class JotifyPlaybackQueue {
	private LinkedList<Track> queue;
	private LinkedList<Track> tracks;
	private LinkedList<Track> history;
	private Track             current;
	
	public JotifyPlaybackQueue(){
		this.queue   = new LinkedList<Track>();
		this.tracks  = new LinkedList<Track>();
		this.history = new LinkedList<Track>();
		this.current = null;
	}
	
	public void queue(Track track){
		this.queue.add(track);
	}
	
	public void add(Track track){
		this.tracks.add(track);
	}
	
	public void addAll(List<Track> tracks){
		this.tracks.addAll(tracks);
	}
	
	public void clear(){
		this.tracks.clear();
		
		this.current = null;
	}
	
	public void clearAll(){
		this.queue.clear();
		this.tracks.clear();
		this.history.clear();
		
		this.current = null;
	}
	
	public void shuffle(){
		Collections.shuffle(this.tracks);
	}
	
	public boolean hasCurrent(){
		return this.current != null;
	}
	
	public boolean hasNext(){
		return !this.queue.isEmpty() || !this.tracks.isEmpty();
	}
	
	public boolean hasPrevious(){
		return !this.history.isEmpty();
	}
	
	public Track current(){
		return this.current;
	}
	
	public Track next(){
		if(this.current != null){
			this.history.addFirst(this.current);
		}
		
		if(!this.queue.isEmpty()){
			this.current = this.queue.removeFirst();
		}
		else if(!this.tracks.isEmpty()){
			this.current = this.tracks.removeFirst();
		}
		else{
			this.current = null;
		}
		
		return this.current;
	}
	
	public Track previous(){
		if(this.current != null){
			this.tracks.addFirst(this.current);
		}
		
		if(!this.history.isEmpty()){
			this.current = this.history.removeFirst();
		}
		else{
			this.current = null;
		}
		
		return this.current;
	}
	
	public List<Track> getQueue(){
		return this.queue;
	}
	
	public List<Track> getTracks(){
		return this.tracks;
	}
	
	public List<Track> getHistory(){
		return this.history;
	}
}
