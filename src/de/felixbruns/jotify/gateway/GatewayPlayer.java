package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.LineUnavailableException;

import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;
import de.felixbruns.jotify.player.SpotifyOggPlayer;
import de.felixbruns.jotify.protocol.Protocol;

public class GatewayPlayer implements Player {
	private Protocol         protocol;
	private SpotifyOggPlayer player;
	private float            volume;
	
	public GatewayPlayer(Protocol protocol){
		if(protocol == null){
			throw new IllegalArgumentException("Protocol can't be 'null'.");
		}
		
		this.protocol = protocol;
		this.player   = null;
		this.volume   = 1.0f;
	}
	
	public void play(Track track, int bitrate, PlaybackListener listener) throws TimeoutException, IOException, LineUnavailableException {
		/* Send play request. */
		try{
			this.protocol.sendPlayRequest();
		}
		catch(ProtocolException e){
			throw new IOException(e);
		}
		
		/* Create channel player. */
		this.player = new SpotifyOggPlayer(this.protocol);
		
		/* Start playing. */
		this.player.play(track, bitrate, listener);
	}
	
	public void play(){
		if(this.player != null){
			this.player.play();
		}
	}
	
	public void pause(){
		if(this.player != null){
			this.player.pause();
		}
	}
	
	public void stop(){
		if(this.player != null){
			this.player.stop();
			
			this.player = null;
		}
	}
	
	public int length(){
		if(this.player != null){
			return this.player.length();
		}
		
		return -1;
	}
	
	public int position(){
		if(this.player != null){
			return this.player.position();
		}
		
		return -1;
	}
	
	public void seek(int ms) throws IOException {
		if(this.player != null){
			this.player.seek(ms);
		}
	}
	
	public float volume(){
		if(this.player != null){
			return this.player.volume();
		}
		
		return -1;
	}
	
	public void volume(float volume){
		this.volume = volume;
		
		if(this.player != null){
			this.player.volume(this.volume);
		}
	}
}
