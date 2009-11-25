package de.felixbruns.jotify;

import java.awt.Image;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.User;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;

public class JotifyPool implements Jotify, Player {
	private List<Jotify>          connectionList;
	private BlockingQueue<Jotify> connectionQueue;
	private Jotify                playConnection;
	private int                   poolSize;
	private String                username;
	private String                password;
	
	private static JotifyPool instance;
	
	static {
		instance = new JotifyPool();
	}
	
	public static JotifyPool getInstance(){
		return instance;
	}
	
	public JotifyPool(){
		this(4);
	}
	
	public JotifyPool(int poolSize){
		this.connectionList  = new LinkedList<Jotify>();
		this.connectionQueue = new LinkedBlockingQueue<Jotify>();
		this.playConnection  = null;
		this.poolSize        = poolSize;
		this.username        = null;
		this.password        = null;
	}
	
	private synchronized Jotify createConnection() throws ConnectionException, AuthenticationException {
		/* Check if username and password are set. */
		if(this.username == null || this.password == null){
			throw new IllegalStateException("Not logged in!");
		}
		
		/* Create a new connection. */
		Jotify connection = new JotifyConnection();
		
		/* Try to login with given username and password. */
		connection.login(this.username, this.password);
		
		/* Start the connections I/O thread. */
		new Thread(connection, "JotifyConnection-Thread").start();
		
		/* Add connection to pool. */
		this.connectionList.add(connection);
		
		return connection;
	}
	
	private void releaseConnection(Jotify connection){
		this.connectionQueue.add(connection);
	}
	
	private Jotify getConnection(){
		Jotify connection;
		
		/* Check if pool size is reached. */
		if(this.connectionList.size() >= this.poolSize){
			/* Try to get a connection from the queue. */
			try{
				connection = this.connectionQueue.poll(10, TimeUnit.SECONDS);
				
				if(connection == null){
					throw new TimeoutException("Couldn't get connection after 10 seconds.");
				}
			}
			catch(InterruptedException e){
				throw new RuntimeException(e);
			}
			catch(TimeoutException e){
				throw new RuntimeException(e);
			}
		}
		else{
			/* Create a new connection. */
			try{
				connection = this.createConnection();
			}
			catch(ConnectionException e){
				throw new RuntimeException(e);
			}
			catch(AuthenticationException e){
				throw new RuntimeException(e);
			}
		}
		
		return connection;
	}
	
	public void login(String username, String password) throws ConnectionException, AuthenticationException {
		/* Check if connections are available. */
		if(!this.connectionList.isEmpty()){
			throw new AuthenticationException("Already logged in!");
		}
		
		this.username = username;
		this.password = password;
		
		/* Create a new connection and immediately add it to the queue. */
		Jotify connection = this.createConnection();
		
		this.releaseConnection(connection);
	}
	
	public void close() throws ConnectionException {
		this.connectionQueue.clear();
		
		/* Close all connections. */
		for(Jotify connection : this.connectionList){
			connection.close();
		}
		
		this.connectionList.clear();
	}
	
	public void run(){
		/* Do nothing. */
	}
	
	public User user(){
		Jotify connection = this.getConnection();
		
		User user = connection.user();
		
		this.releaseConnection(connection);
		
		return user;
	}
	
	public Result toplist(String type, String region, String username) {
		Jotify connection = this.getConnection();
		
		Result result = connection.toplist(type, region, username);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	public Result search(String query){
		Jotify connection = this.getConnection();
		
		Result result = connection.search(query);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	
	public Image image(String id){
		Jotify connection = this.getConnection();
		
		Image image = connection.image(id);
		
		this.releaseConnection(connection);
		
		return image;
	}
	
	public Artist browse(Artist artist){
		Jotify connection = this.getConnection();
		
		artist = connection.browse(artist);
		
		this.releaseConnection(connection);
		
		return artist;
	}
	
	public Album browse(Album album){
		Jotify connection = this.getConnection();
		
		album = connection.browse(album);
		
		this.releaseConnection(connection);
		
		return album;
	}
	
	public Result browse(Track track){
		Jotify connection = this.getConnection();
		
		Result result = connection.browse(track);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	public Result browse(List<Track> tracks){
		Jotify connection = this.getConnection();
		
		Result result = connection.browse(tracks);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	public Album browseAlbum(String id){
		Jotify connection = this.getConnection();
		
		Album album = connection.browseAlbum(id);
		
		this.releaseConnection(connection);
		
		return album;
	}
	
	public Artist browseArtist(String id){
		Jotify connection = this.getConnection();
		
		Artist artist = connection.browseArtist(id);
		
		this.releaseConnection(connection);
		
		return artist;
	}
	
	public Result browseTrack(String id){
		Jotify connection = this.getConnection();
		
		Result result = connection.browseTrack(id);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	public Result browseTracks(List<String> tracks){
		Jotify connection = this.getConnection();
		
		Result result = connection.browseTracks(tracks);
		
		this.releaseConnection(connection);
		
		return result;
	}
	
	public PlaylistContainer playlists(){
		Jotify connection = this.getConnection();
		
		PlaylistContainer playlists = connection.playlists();
		
		this.releaseConnection(connection);
		
		return playlists;
	}
	
	public boolean playlistsAddPlaylist(PlaylistContainer playlists, Playlist playlist){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistsAddPlaylist(playlists, playlist);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistsAddPlaylist(PlaylistContainer playlists, Playlist playlist, int position){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistsAddPlaylist(playlists, playlist, position);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public Playlist playlist(String id, boolean cached){
		Jotify connection = this.getConnection();
		
		Playlist playlist = connection.playlist(id, cached);
		
		this.releaseConnection(connection);
		
		return playlist;
	}
	
	public Playlist playlist(String id){
		return this.playlist(id, false);
	}
	
	public Playlist playlistCreate(String name, boolean collaborative){
		Jotify connection = this.getConnection();
		
		Playlist playlist = connection.playlistCreate(name, collaborative);
		
		this.releaseConnection(connection);
		
		return playlist;
	}
	
	public boolean playlistAddTrack(Playlist playlist, Track track, int position){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistAddTrack(playlist, track, position);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistAddTracks(Playlist playlist, List<Track> tracks, int position){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistAddTracks(playlist, tracks, position);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistRemoveTrack(Playlist playlist, int position){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistRemoveTrack(playlist, position);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistRemoveTracks(Playlist playlist, int position, int count){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistRemoveTracks(playlist, position, count);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistRename(Playlist playlist, String name){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistRename(playlist, name);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public boolean playlistSetCollaborative(Playlist playlist, boolean collaborative){
		Jotify connection = this.getConnection();
		
		boolean success = connection.playlistSetCollaborative(playlist, collaborative);
		
		this.releaseConnection(connection);
		
		return success;
	}
	
	public void play(Track track, PlaybackListener listener){
		if(this.playConnection != null){
			this.playConnection.stop();
			
			this.releaseConnection(this.playConnection);
			
			this.playConnection = null;
		}
		
		this.playConnection = this.getConnection();
		
		this.playConnection.play(track, listener);
	}
	
	public void play(){
		if(this.playConnection != null){
			this.playConnection.play();
		}
	}
	
	public void pause(){
		if(this.playConnection != null){
			this.playConnection.pause();
		}
	}
	
	public void stop(){
		if(this.playConnection != null){
			this.playConnection.stop();
			
			this.releaseConnection(this.playConnection);
			
			this.playConnection = null;
		}
	}
	
	public int length(){
		if(this.playConnection != null){
			return this.playConnection.length();
		}
		
		return -1;
	}
	
	public int position(){
		if(this.playConnection != null){
			return this.playConnection.position();
		}
		
		return -1;
	}
	
	public float volume(){
		if(this.playConnection != null){
			return this.playConnection.volume();
		}
		
		return Float.NaN;
	}
	
	public void volume(float volume){
		if(this.playConnection != null){
			this.playConnection.volume(volume);
		}
	}
}
