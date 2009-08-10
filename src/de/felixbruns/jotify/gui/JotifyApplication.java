package de.felixbruns.jotify.gui;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.JotifyPool;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.swing.JotifyFrame;
import de.felixbruns.jotify.gui.swing.JotifyLoginDialog;
import de.felixbruns.jotify.gui.util.JotifyLoginCredentials;
import de.felixbruns.jotify.gui.util.JotifyPreferences;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;

public class JotifyApplication {
	/* Application frame and broadcast. */
	private JotifyFrame       frame;
	private JotifyBroadcast   broadcast;
	private JotifyPreferences settings;
	private JotifyPlayer      player;
	
	/* Jotify API. */
	private final Jotify jotify;
	
	public JotifyApplication(final Jotify jotify){
		this.broadcast = JotifyBroadcast.getInstance();
		this.frame     = null; /* Created in initialize method. */
		this.settings  = null; /* Created in initialize method. */
		this.player    = null; /* Created in initialize method. */
		this.jotify    = jotify;
	}
	
	public void initialize(){
		/* Load settings. */
		this.settings = JotifyPreferences.getInstance();
		this.settings.load();
		
		/* Get login credentials from settings. */
		JotifyLoginCredentials credentials = this.settings.getLoginCredentials();
		
		/* Login process. Repeat if login failed. */
		while(true){
			/* Ask for login credentials if they're not saved. */
			if(credentials == null || !credentials.getRemember()){
				credentials = JotifyLoginDialog.showDialog();
				
				JotifyLoginDialog.hideMessage();
				JotifyLoginDialog.showLoader();
			}
			/* If credentials are present, just show loader. */
			else{
				JotifyLoginDialog.showDialogNonBlocking();
				JotifyLoginDialog.setLoginCredentials(credentials);
				JotifyLoginDialog.showLoader();
			}
			
			try{
				/* Try to login by getting the default Jotify instance. */
				this.jotify.login(credentials.getUsername(), credentials.getPassword());
				
				/* Create player. */
				this.player = new JotifyPlayer(jotify);
				this.broadcast.addControlListener(this.player);
				
				/* Create scrobbler if enabled. */
				if(this.settings.getBoolean("lastfm.enabled", false)){
					JotifyScrobbler scrobbler = new JotifyScrobbler(
						this.settings.getString("lastfm.username"),
						this.settings.getString("lastfm.password")
					);
					this.player.addPlaybackListener(scrobbler);
				}
				
				/* Save login credentials (they are valid!). */
				this.settings.setLoginCredentials(credentials);
				this.settings.save();
				
				/* Create and show main application frame. Center it on screen. */
				this.frame = new JotifyFrame(jotify);
				this.frame.setVisible(true);
				this.frame.setLocationRelativeTo(null);
				
				/* Load playlists in a separate thread. */
				new Thread("Playlist-Loading-Thread"){
					public void run(){
						/* Get information about account playlists. */
						PlaylistContainer playlists = jotify.playlists();
						//boolean           useCache  = true;
						
						/* Fire playlist added events. */
						for(Playlist playlist : playlists){
							broadcast.firePlaylistAdded(playlist);
						}
						
						/* Check for changes in playlists. */
						if(playlists.getRevision() > settings.getPlaylistsRevision()){
							settings.setPlaylistsRevision(playlists.getRevision());
							settings.save();
							
							//useCache = false;
						}
						
						/* Get details for each playlist. */
						for(Playlist playlist : playlists){
							/* Get playlist details. */
							//playlist = jotify.playlist(playlist.getId(), useCache); TODO load from cache
							playlist = jotify.playlist(playlist.getId());
							
							if (!playlist.getTracks().isEmpty()) {
					  		/* Browse multiple tracks and add them to the playlist. */
  							Result result = jotify.browse(playlist.getTracks());
							
  							/* Add track information to playlist. */
  							for(Track track : result.getTracks()){
  								int index = playlist.getTracks().indexOf(track);
								
  								if(index != -1){
  									playlist.getTracks().set(index, track);
  								}
  							}
						  }
							
							/* Fire playlist updated events. */
							broadcast.firePlaylistUpdated(playlist);
						}
					}
				}.start();
				
				/* We're logged in now. Don't ask for credentials again. */
				break;
			}
			/* If we got a connection error, show it. */
			catch(ConnectionException e){
				JotifyLoginDialog.hideLoader();
				JotifyLoginDialog.showError(e.getMessage());
			}
			/* If we got an authentication error, show it. */
			catch(AuthenticationException e){
				JotifyLoginDialog.hideLoader();
				JotifyLoginDialog.showError(e.getMessage());
				
				/* Don't check "Remember me", if login failed. */
				credentials.setRemember(false);
			}
		}
		
		/* Hide login dialog. */
		JotifyLoginDialog.hideLoader();
		JotifyLoginDialog.hideDialog();
	}
	
	/* Main entry point of program. Create application and initialize it. */
	public static void main(String[] args) throws Exception {
		JotifyApplication application = new JotifyApplication(new JotifyPool());
		
		application.initialize();
	}
}
