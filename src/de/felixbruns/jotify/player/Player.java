package de.felixbruns.jotify.player;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.*;

public class Player {
	private AudioInputStream audioStream;
	private SourceDataLine   audioLine;
	private boolean          active;
	private boolean          complete;
	private Semaphore        semaphore;
	
	public Player(){
		this.audioStream = null;
		this.audioLine   = null;
		this.active      = false;
		this.complete    = true;
		this.semaphore   = new Semaphore(1);
		
		/* Acquire permit. Status is paused. */
		this.semaphore.acquireUninterruptibly();
	}
	
	public boolean open(final InputStream stream){
		/* Close previously open audio line */
		if(this.check()){
			/* Close player */
			this.close();
		}
		
		/* Skip first 167 bytes (Spotify specific header). TODO: Decode using SpotifyOggHeader. */
		try{
			stream.skip(167);
		}
		catch(IOException e){
			return false;
		}
		
		/* Try to get audio input stream and source/target formats. */
		AudioInputStream sourceStream = null;
		AudioFormat      sourceFormat = null;
		AudioFormat      targetFormat = null;
		
		try{
			/* Get source stream */
			sourceStream = AudioSystem.getAudioInputStream(stream);
			
			/* Get source format and set target format */
			sourceFormat = sourceStream.getFormat();
			targetFormat = new AudioFormat(
				sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), true, false
			);
		}
		catch(UnsupportedAudioFileException e){
			return false;
		}
		catch(IOException e){
			return false;
		}
		
		/* Get target stream */
		this.audioStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
		
		/* Get line info for target format */
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
		
		try{
			/* Get line for obtained line info */
			this.audioLine = (SourceDataLine)AudioSystem.getLine(info);
			
			/* Open line for playback */
			this.audioLine.open();
		}
		catch(LineUnavailableException e){
			return false;
		}
		
		/* Set status */
		this.active   = true;
		this.complete = false;
		
		/* Start a thread which writes data to the line */
		(new Thread(){
			public void run(){
				/* Buffer for data and number of bytes read */
				byte[] buffer = new byte[1024];
				int	readBytes = 0;
				
				/* Read-write loop */
				while(active && readBytes != -1){
					/* Wait if we're stopped. */
					semaphore.acquireUninterruptibly();
					
					/* Read data from stream */
					try{
						readBytes = audioStream.read(buffer, 0, buffer.length);
					}
					catch(IOException e){
						e.printStackTrace();
					}
					
					/* Write data to line */
					if(readBytes >= 0){
						audioLine.write(buffer, 0, readBytes);
					}
					
					/* Release permit, so we can be stopped. */
					semaphore.release();
				}
				
				/* Block until all data is processed */
				audioLine.drain();
				
				/* Set status */
				active   = false;
				complete = true;
				
				/* Close line */
				audioLine.close();
			}
		}).start();
		
		/* Success. */
		return true;
	}
	
	public void play(){
		if(this.check()){
			/* Release permit to resume playing thread. */
			this.semaphore.release();
			
			this.audioLine.start();
		}
	}
	
	public void stop(){
		if(this.check()){
			/* Acquire a permit to stop playing thread. */
			this.semaphore.acquireUninterruptibly();
			
			this.audioLine.stop();
		}
	}
	
	public int position(){
		if(this.check()){
			return (int)(this.audioLine.getMicrosecondPosition() / 1000000);
		}
		
		return 0;
	}
	
	public boolean isComplete(){
		return this.complete;
	}
	
	public void close(){
		if(this.check()){
			this.active = false;
			
			/* Wait for line to become free */
			while(this.audioLine.isOpen());
		}
	}
	
	private boolean check(){
		return this.audioLine != null && this.audioLine.isOpen();
	}
}
