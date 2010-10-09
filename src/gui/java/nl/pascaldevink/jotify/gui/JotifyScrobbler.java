package nl.pascaldevink.jotify.gui;

import java.io.IOException;

import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;

import net.roarsoftware.lastfm.scrobble.Scrobbler;
import net.roarsoftware.lastfm.scrobble.Source;

public class JotifyScrobbler implements PlaybackListener {
	private Scrobbler scrobbler;
	private boolean   scrobbled;
	private int       startTime;
	private int       position;
	
	public JotifyScrobbler(String username, String password) throws AuthenticationException {
		this.scrobbler = Scrobbler.newScrobbler("jty", "1.0", username);
		this.scrobbled = false;
		this.startTime = 0;
		this.position  = 0;
		
		try{
			if(!this.scrobbler.handshake(password).ok()){
				throw new AuthenticationException("Scrobbler handshake failed!");
			}
		}
		catch(IOException e){
			throw new AuthenticationException("Scrobbler handshake failed!");
		}
	}
	
	public void playbackStarted(Track track){
		/* Set scrobble status and time. */
		this.scrobbled = false;
		this.startTime = (int)(System.currentTimeMillis() / 1000);
		this.position  = 0;
		
		/* Set now playing. */
		try{
			System.out.println("Now playing: " + track.getArtist().getName() + "-" + track.getTitle());
			
			this.scrobbler.nowPlaying(
				track.getArtist().getName(), track.getTitle(), null,
				track.getLength() / 1000, -1
			);
		}
		catch(IOException e){
			/* Ignore. */
		}
		catch(NullPointerException e){
			/* Ignore. */
		}
	}
	
	public void playbackStopped(Track track){
		this.submit(track);
	}
	
	public void playbackResumed(Track track){
		/* Nothing. */
	}
	
	public void playbackPosition(Track track, int position){
		this.position = position;
	}
	
	public void playbackFinished(Track track){
		this.submit(track);
	}
	
	private boolean submit(Track track){
		/* Only continue, if track wasn't scrobbled yet. */
		if(this.scrobbled){
			return false;
		}
		
		/* And if track was played more than 240 seconds or half it's length. */
		if(this.position < 240 && this.position < track.getLength() / 2000){
			return false;
		}
		
		/* Submit track. */
		try{
			System.out.println("Submit: " + track.getArtist().getName() + "-" + track.getTitle());
			
			if(this.scrobbler.submit(
				track.getArtist().getName(), track.getTitle(), null,
				track.getLength() / 1000, -1, Source.USER, this.startTime
			).ok()){
				this.scrobbled = true;
				
				return true;
			}
			
			return false;
		}
		catch(IOException e){
			return false;
		}
	}
}
