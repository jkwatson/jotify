package de.felixbruns.jotify.gui.listeners;

import java.util.ArrayList;
import java.util.List;

import de.felixbruns.jotify.gui.JotifyPlaybackQueue;
import de.felixbruns.jotify.gui.listeners.PlayerListener.Status;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;

public class JotifyBroadcast {
	private List<PlaylistListener> playlistListeners;
	private List<QueueListener>    queueListeners;
	private List<SearchListener>   searchListeners;
	private List<BrowseListener>   browseListeners;
	private List<ControlListener>  controlListeners;
	private List<PlayerListener>   playerListeners;
	
	private List<ClearSelectionListener> clearSelectionListener;
	
	private static JotifyBroadcast instance;
	
	static {
		instance = new JotifyBroadcast();
	}
	
	public static JotifyBroadcast getInstance(){
		return instance;
	}
	
	private JotifyBroadcast(){
		this.playlistListeners = new ArrayList<PlaylistListener>();
		this.queueListeners    = new ArrayList<QueueListener>();
		this.searchListeners   = new ArrayList<SearchListener>();
		this.browseListeners   = new ArrayList<BrowseListener>();
		this.controlListeners  = new ArrayList<ControlListener>();
		this.playerListeners   = new ArrayList<PlayerListener>();

		this.clearSelectionListener = new ArrayList<ClearSelectionListener>();
	}
	
	public void addPlaylistListener(PlaylistListener listener){
		this.playlistListeners.add(listener);
	}
	
	public void addQueueListener(QueueListener listener){
		this.queueListeners.add(listener);
	}
	
	public void addSearchListener(SearchListener listener){
		this.searchListeners.add(listener);
	}
	
	public void addBrowseListener(BrowseListener listener){
		this.browseListeners.add(listener);
	}
	
	public void addControlListener(ControlListener listener){
		this.controlListeners.add(listener);
	}
	
	public void addPlayerListener(PlayerListener listener){
		this.playerListeners.add(listener);
	}
	
	public void addClearSelectionListener(ClearSelectionListener listener){
		this.clearSelectionListener.add(listener);
	}
	
	public void firePlaylistAdded(Playlist playlist) {
		for(PlaylistListener listener : this.playlistListeners){
			listener.playlistAdded(playlist);
		}
	}
	
	public void firePlaylistRemoved(Playlist playlist) {
		for(PlaylistListener listener : this.playlistListeners){
			listener.playlistRemoved(playlist);
		}
	}
	
	public void firePlaylistUpdated(Playlist playlist) {
		for(PlaylistListener listener : this.playlistListeners){
			listener.playlistUpdated(playlist);
		}
	}
	
	public void firePlaylistSelected(Playlist playlist) {
		for(PlaylistListener listener : this.playlistListeners){
			listener.playlistSelected(playlist);
		}
	}
	
	public void fireQueueSelected(JotifyPlaybackQueue queue) {
		for(QueueListener listener : this.queueListeners){
			listener.queueSelected(queue);
		}
	}
	
	public void fireQueueUpdated(JotifyPlaybackQueue queue) {
		for(QueueListener listener : this.queueListeners){
			listener.queueUpdated(queue);
		}
	}
	
	public void fireSearchResultReceived(Result result){
		for(SearchListener listener : this.searchListeners){
			listener.searchResultReceived(result);
		}
	}
	
	public void fireSearchResultSelected(Result result){
		for(SearchListener listener : this.searchListeners){
			listener.searchResultSelected(result);
		}
	}
	
	public void fireBrowsedArtist(Artist artist){
		for(BrowseListener listener : this.browseListeners){
			listener.browsedArtist(artist);
		}
	}
	
	public void fireBrowsedAlbum(Album album){
		for(BrowseListener listener : this.browseListeners){
			listener.browsedAlbum(album);
		}
	}
	
	public void fireBrowsedTracks(Result result){
		for(BrowseListener listener : this.browseListeners){
			listener.browsedTracks(result);
		}
	}
	
	public void fireControlPlay(){
		for(ControlListener listener : this.controlListeners){
			listener.controlPlay();
		}
	}
	
	public void fireControlPause(){
		for(ControlListener listener : this.controlListeners){
			listener.controlPause();
		}
	}
	
	public void fireControlPrevious(){
		for(ControlListener listener : this.controlListeners){
			listener.controlPrevious();
		}
	}
	
	public void fireControlNext(){
		for(ControlListener listener : this.controlListeners){
			listener.controlNext();
		}
	}
	
	public void fireControlVolume(float volume){
		for(ControlListener listener : this.controlListeners){
			listener.controlVolume(volume);
		}
	}
	
	public void fireControlSeek(float percent){
		for(ControlListener listener : this.controlListeners){
			listener.controlSeek(percent);
		}
	}
	
	public void fireControlSelect(Track track){
		for(ControlListener listener : this.controlListeners){
			listener.controlSelect(track);
		}
	}
	
	public void fireControlSelect(List<Track> tracks){
		for(ControlListener listener : this.controlListeners){
			listener.controlSelect(tracks);
		}
	}
	
	public void fireControlQueue(Track track){
		for(ControlListener listener : this.controlListeners){
			listener.controlQueue(track);
		}
	}
	
	public void firePlayerTrackChanged(Track track){
		for(PlayerListener listener : this.playerListeners){
			listener.playerTrackChanged(track);
		}
	}
	
	public void firePlayerStatusChanged(Status status){
		for(PlayerListener listener : this.playerListeners){
			listener.playerStatusChanged(status);
		}
	}
	
	public void firePlayerPositionChanged(int position){
		for(PlayerListener listener : this.playerListeners){
			listener.playerPositionChanged(position);
		}
	}
	
	public void fireClearSelection(){
		for(ClearSelectionListener listener : this.clearSelectionListener){
			listener.clearSelection();
		}
	}
}
