package de.felixbruns.jotify.gui;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

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
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;

public class JotifyApplication {
	/* Application frame and broadcast. */
	private JotifyFrame       frame;
	private JotifyBroadcast   broadcast;
	private JotifyPreferences settings;
	private JotifyPlayer      player;
	private TrayIcon          trayIcon;
	
	/* Jotify API. */
	private final Jotify jotify;
	
	public JotifyApplication(final Jotify jotify){
		this.broadcast = JotifyBroadcast.getInstance();
		this.frame     = null; /* Created in initialize method. */
		this.settings  = null; /* Created in initialize method. */
		this.player    = null; /* Created in initialize method. */
		this.jotify    = jotify;
		this.trayIcon  = null; /* Created in initialize method. */
	}
	
	public void initialize(){
		/* Load settings. */
		this.settings = JotifyPreferences.getInstance();
		this.settings.load();
		
		/* Get login credentials from settings. */
		final JotifyLoginCredentials credentials = this.settings.getLoginCredentials();
		
		/* Show login dialog. */
		JotifyLoginDialog.showDialog();
		
		/* Login process. Repeat if login failed. */
		while(true){
			/* Ask for login credentials if they're not marked as remebmered. */
			if(!credentials.getRemember()){
				JotifyLoginDialog.getLoginCredentials(credentials);
			}
			/* If credentials are present, just show loader. */
			else{
				JotifyLoginDialog.setLoginCredentials(credentials);
			}
			
			/* Hide message and show loader. */
			JotifyLoginDialog.hideMessage();
			JotifyLoginDialog.showLoader();
			JotifyLoginDialog.updateDialog();
			
			try{
				/* Try to login by getting the default Jotify instance. */
				this.jotify.login(credentials.getUsername(), credentials.getPassword());
				
				/* Check if user is premium. */
				if(!this.jotify.user().isPremium()){
					this.jotify.close();
					
					throw new AuthenticationException("You need a premium account in order to use jotify!");
				}
				
				/* Create player. */
				this.player = new JotifyPlayer(this.jotify);
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
				this.frame = new JotifyFrame(this.jotify);
				this.frame.setVisible(true);
				this.frame.setLocationRelativeTo(null);
				
				/* Add a system tray icon. */
				if(SystemTray.isSupported()){
					this.trayIcon = new TrayIcon(
						new ImageIcon(JotifyApplication.class.getResource("images/icon_128.png")).getImage(),
						"Jotify"
					);
					
					/* Add a popup menu to the system tray icon (it for some reason blocks the whole application when shown). */
					final PopupMenu menu       = new PopupMenu();
					final MenuItem  trackItem  = new MenuItem("No track playing");
					final MenuItem  openItem   = new MenuItem("Open Jotify");
					//final MenuItem  playItem   = new MenuItem("Play/Pause");
					//final MenuItem  nextItem   = new MenuItem("Next");
					//final MenuItem  prevItem   = new MenuItem("Previous");
					final MenuItem  logoutItem = new MenuItem("Logout '" + this.jotify.user().getName() + "'");
					final MenuItem  exitItem   = new MenuItem("Exit");
					
					trackItem.setEnabled(false);
					//nextItem.setEnabled(false);
					//prevItem.setEnabled(false);
					
					openItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							frame.setExtendedState(frame.getExtendedState() &~ JFrame.ICONIFIED);
							frame.requestFocus();
						}
					});
					
					logoutItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							credentials.setRemember(false);
							
							/* Save login credentials (they are valid!). */
							settings.setLoginCredentials(credentials);
							settings.save();
							
							System.exit(0);
						}
					});
					
					exitItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							System.exit(0);
						}
					});
					
					menu.add(trackItem);
					menu.addSeparator();
					menu.add(openItem);
					//menu.addSeparator();
					//menu.add(playItem);
					//menu.add(nextItem);
					//menu.add(prevItem);
					menu.addSeparator();
					menu.add(logoutItem);
					menu.add(exitItem);
					
					this.player.addPlaybackListener(new PlaybackListener(){
						public void playbackFinished(Track track){}
						public void playbackPosition(Track track, int position){}
						public void playbackResumed(Track track){}
						public void playbackStarted(Track track){
							trackItem.setLabel(track.getArtist().getName() + " - " + track.getTitle());
						}
						public void playbackStopped(Track track){}
					});
					
					this.trayIcon.setImageAutoSize(true);
					this.trayIcon.setPopupMenu(menu);
					
					try{
						SystemTray.getSystemTray().add(this.trayIcon);
					}
					catch(AWTException e){
						/* Couldn't add system tray icon. */
					}
				}
				
				/* Load playlists in a separate thread. TODO: retry on fail. */
				new Thread("Playlist-Loading-Thread"){
					public void run(){
						/* Get information about account playlists. */
						PlaylistContainer playlists = null;
						boolean           cached    = true;
						
						while(true){
							try{
								playlists = jotify.playlistContainer();
								
								break;
							}
							catch(TimeoutException e){
								continue;
							}
						}
						
						/* Fire playlist added events. */
						for(Playlist playlist : playlists){
							broadcast.firePlaylistAdded(playlist);
						}
						
						/* Check for changes in playlists. */
						if(playlists.getRevision() > settings.getPlaylistsRevision()){
							settings.setPlaylistsRevision(playlists.getRevision());
							settings.save();
							
							cached = false;
						}
						
						/* Get details for each playlist. */
						for(Playlist playlist : playlists){
							/* Get playlist details. */
							while(true){
								try{
									playlist = jotify.playlist(playlist.getId(), cached);
									
									break;
								}
								catch(TimeoutException e){
									continue;
								}
							}
							
							/* If playlist contains tracks, browse for track information. */
							if(!playlist.getTracks().isEmpty()){
								int totalTracks = playlist.getTracks().size();
								int numTracks   = 200;
								int numRequests = (int)(totalTracks / numTracks) + 1;
								
								/* Browse for 200 tracks at a time tracks and add them to the playlist. */
								for(int i = 0; i < numRequests; i++){
									List<Track> tracks = null;
									
									while(true){
										try{
											tracks = jotify.browse(
												playlist.getTracks().subList(
													i * numTracks,
													Math.min((i + 1) * numTracks, totalTracks)
												)
											);
											
											break;
										}
										catch(TimeoutException e){
											continue;
										}
									}
									
									/* Add track information to playlist (also works with duplicate tracks). */
									for(Track track : tracks){
		  								for(int j = 0; j < playlist.getTracks().size(); j++){
		  									if(track.equals(playlist.getTracks().get(j))){
		  										playlist.getTracks().set(j, track);
		  									}
		  								}
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
				if(e.getCause() != null){
					JotifyLoginDialog.showErrorMessage(e.getCause().getMessage());
				}
				else{
					JotifyLoginDialog.showErrorMessage(e.getMessage());
				}
				
				JotifyLoginDialog.hideLoader();
				JotifyLoginDialog.updateDialog();
			}
			/* If we got an authentication error, show it. */
			catch(AuthenticationException e){
				if(e.getCause() != null){
					JotifyLoginDialog.showErrorMessage(e.getCause().getMessage());
				}
				else{
					JotifyLoginDialog.showErrorMessage(e.getMessage());
				}
				
				JotifyLoginDialog.hideLoader();
				JotifyLoginDialog.updateDialog();
				
				/* Don't check "Remember me", if login failed. */
				credentials.setRemember(false);
			}
			/* If we got a timeout. */
			catch(TimeoutException e){
				JotifyLoginDialog.showErrorMessage(e.getMessage());
				
				JotifyLoginDialog.hideLoader();
				JotifyLoginDialog.updateDialog();
			}
		}
		
		/* Hide login dialog. */
		JotifyLoginDialog.hideDialog();
	}
	
	/* Main entry point of program. Create application and initialize it. */
	public static void main(String[] args) throws Exception {
		JotifyApplication application = new JotifyApplication(new JotifyPool());
		
		application.initialize();
	}
}
