package de.felixbruns.jotify.player.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;
import de.felixbruns.jotify.player.SpotifyOggHeader;
import de.felixbruns.jotify.util.MathUtilities;

public class HTTPStreamPlayer implements Runnable, Player {
	/* Streams, audio decoding and output. */
	private SpotifyOggHeader  spotifyOggHeader;
	private AudioInputStream  audioStream;
	private SourceDataLine    audioLine;
	
	/* Playback listener and playback position. */
	private PlaybackListener listener;
	private long             position;
	
	/* 
	 * Protocol, Track object and variables for
	 * substream requesting and handling.
	 */
	private Track track;
	
	/* Current player status and semaphores for pausing. */
	private boolean   active;
	private Semaphore pause;
	
	public HTTPStreamPlayer(List<String> urls, Track track, byte[] key, PlaybackListener listener){
		/* Set protocol, track, playback listener and cache. */
		this.track    = track;
		this.listener = listener;
		this.position = 0;
		
		/* Audio will be initialized in "open" method. */
		this.spotifyOggHeader = null;
		this.audioStream      = null;
		this.audioLine        = null;
		this.active           = false;
		this.pause            = new Semaphore(1);
		
		/* Acquire permit. Status is paused. */
		this.pause.acquireUninterruptibly();
		
		/* Build a list of input streams. */
		Collection<InputStream> streams = new ArrayList<InputStream>();
		
		for(String url : urls){
			try{
				/* Open connection to HTTP stream URL and set request headers. */
				URLConnection connection = new URL(url).openConnection();
				
				connection.setRequestProperty("User-Agent", "Jotify-Java/0.1/99998");
				connection.setRequestProperty(
					"Range", "bytes=0-" + Math.round(160.0 * 1024.0 * track.getLength() / 1000.0 / 8.0 / 4.0)
				);
				
				streams.add(connection.getInputStream());
			}
			catch(IOException e){
				throw new RuntimeException("Can't open connection to HTTP stream!", e);
			}
		}
		
		InputStream audioStream = new BufferedInputStream(
			new DecryptingInputStream(
				new DeinterleavingInputStream(streams),
				key
			)
		);
		
		/* Open input stream for playing. */
		if(!this.open(audioStream)){
			throw new RuntimeException("Can't open input stream for playing!");
		}
	}
	
	/* Open an input stream and start decoding it,
	 * set up audio stuff when AudioInputStream
	 * was sucessfully created.
	 */
	private boolean open(InputStream stream){
		/* Audio streams and formats. */
		AudioInputStream sourceStream;
		AudioFormat      sourceFormat;
		AudioFormat      targetFormat;
		
		/* Spotify specific ogg header. */
		byte[] header = new byte[167];
		
		try{
			/* Read and decode header. */
			stream.read(header);
			
			this.spotifyOggHeader = new SpotifyOggHeader(header);
			
			/* Get audio source stream */
			sourceStream = AudioSystem.getAudioInputStream(stream);
			
			/* Get source format and set target format. */
			sourceFormat = sourceStream.getFormat();
			targetFormat = new AudioFormat(
				sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), true, false
			);
			
			/* Get target audio stream */
			this.audioStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
			
			/* Get line info for target format. */
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
			
			/* Get line for obtained line info. */
			this.audioLine = (SourceDataLine)AudioSystem.getLine(info);
			
			/* Finally open line for playback. */
			this.audioLine.open();
		}
		catch(UnsupportedAudioFileException e){
			e.printStackTrace();
			return false;
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
		catch(LineUnavailableException e){
			e.printStackTrace();
			return false;
		}
		
		/* Set player status. */
		this.active = true;
		
		/* Start thread which writes data to the line. */
		new Thread(this, "ChannelPlayer-Thread").start();
		
		/* Success. */
		return true;
	}
	
	public void run(){
		/* Buffer for data and number of bytes read */
		byte[] buffer = new byte[1024];
		int read = 0;
		
		this.position = 0;
		
		/* Fire playback started and stopped events (track is paused). */
		if(this.listener != null){
			this.listener.playbackStarted(this.track);
			this.listener.playbackStopped(this.track);
		}
		
		/* Read-write loop. */
		while(this.active && read != -1){
			/* Wait if we're paused. */
			this.pause.acquireUninterruptibly();
			
			/* Read data from audio stream and write it to the audio line. */
			try{
				if((read = this.audioStream.read(buffer, 0, buffer.length)) > 0){
					this.audioLine.write(buffer, 0, read);
				}
			}
			catch(IOException e){
				e.printStackTrace();
				
				/* Don't care. */
			}
			
			/* Get current playback position. */
			long position = this.audioLine.getMicrosecondPosition();
			
			/* Fire playback position event about every 100 ms. */
			if(this.listener != null && position - this.position > 100000){
				this.listener.playbackPosition(this.track, (int)(position / 1000000));
				
				/* Update last postition. */
				this.position = position;
			}
			
			/* Release permit, so we can be paused. */
			this.pause.release();
		}
		
		/* Block until all data is processed, then close audio line. */
		this.audioLine.drain();
		this.audioLine.close();
		
		/* Fire playback finished event. (Note: Not when closed manually!) */
		if(this.listener != null && this.active){
			this.listener.playbackFinished(this.track);
		}
		
		/* Set player status. */
		this.active = false;
	}
	
	public void play(Track track, PlaybackListener listener){
		this.play();
	}
	
	/**
	 * Start playback or continue playing if "stop" was called before.
	 */
	public void play(){
		/* Start audio line again. */
		this.audioLine.start();
		
		/* Release permit to resume IO thread. */
		this.pause.release();
		
		/* Fire playback resumed event. */
		if(this.listener != null){
			this.listener.playbackResumed(this.track);
		}
	}
	
	/**
	 * Stop playback of audio until "play" is called again.
	 */
	public void pause(){
		/* Stop audio line. */
		this.audioLine.stop();
		
		/* Acquire a permit to stop IO thread. */
		this.pause.acquireUninterruptibly();
		
		/* Fire playback stopped event. */
		if(this.listener != null){
			this.listener.playbackStopped(this.track);
		}
	}
	
	/**
	 * Return the total length of the audio stream in seconds (if available).
	 * This information is loaded from the Spotify specific ogg header.
	 * 
	 * @return Length of audio stream in seconds or -1 if not available.
	 */
	public int length(){
		/* TODO: Remove hard-coded sample rate!? */
		if(this.spotifyOggHeader != null){
			return this.spotifyOggHeader.getSeconds(44100);
		}
		
		return -1;
	}
	
	/**
	 * Return the current playback position in seconds.
	 * 
	 * @return Position of playback in seconds.
	 */
	public int position(){
		return (int)(this.position / 1000000);
	}
	
	/**
	 * Get current volume value.
	 * 
	 * @return A value between 0.0 to 1.0.
	 */
	public float volume(){
		float gain;
		float volume;
		
		/* Get gain control. */
		FloatControl control = (FloatControl)this.audioLine.getControl(FloatControl.Type.MASTER_GAIN);
		
		/* Get gain and constrain it. */
		gain = MathUtilities.constrain(
			control.getValue(), control.getMinimum(), 0.0f
		);
		
		/* Calculate volume from gain. */
		if(gain == control.getMinimum()){
			volume = 0.0f;
		}
		else{
			volume = (float)Math.pow(10.0f, (gain / 20.0f) * 1.0f);
		}
		
		/* Return volume value. */
		return volume;
	}
	
	/**
	 * Set volume of audio line.
	 * 
	 * @param volume A value from 0.0 to 1.0.
	 */
	public void volume(float volume){
		float gain;
		
		/* Check arguments. */
		if(volume < 0.0f || volume > 1.0f){
			throw new IllegalArgumentException("Volume has to be a value from 0.0 to 1.0!");
		}
		
		/* Get gain control. */
		FloatControl control = (FloatControl)this.audioLine.getControl(FloatControl.Type.MASTER_GAIN);
		
		/* 
		 * Calculate gain from volume:
		 * 
		 * 100% volume =   0 dB
		 *  50% volume = - 6 dB
		 *  10% volume = -20 dB
		 *   1% volume = -40 dB
		 *   0% volume = min dB
		 */
		if(volume == 0.0){
			gain = control.getMinimum();
		}
		else{
			gain = 20.0f * (float)Math.log10(volume / 1.0f);
		}
		
		/* Set volume/gain (constrain it before). */
		control.setValue(MathUtilities.constrain(
			gain, control.getMinimum(), control.getMaximum()
		));
	}
	
	/**
	 * Close audio line and stream which will stop playing. Playing can't
	 * be resumed after that, use the "stop" method for that functionality.
	 */
	public void stop(){
		this.active = false;
		
		try{
			this.audioStream.close();
			this.audioLine.close();
		}
		catch(Exception e){
			/* Don't care. Catch IOException and NullPointerException. */
		}
		
		/* Fire playback stopped event. */
		if(this.listener != null){
			this.listener.playbackStopped(this.track);
		}
	}
}
