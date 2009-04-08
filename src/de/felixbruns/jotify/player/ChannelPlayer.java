package de.felixbruns.jotify.player;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;

public class ChannelPlayer implements ChannelListener {
	/* Decryption stuff. */
	private Cipher            cipher;
	private Key               key;
	private byte[]            iv;
	
	/* Requesting and loading stuff. */
	private Track             track;
	private Protocol          protocol;
	private PipedInputStream  input;
	private PipedOutputStream output;
	private int               offset;
	private int               length;
	private int               totalLength;
	private boolean           loading;
	
	/* Audio playing stuff. */
	private AudioInputStream audioStream;
	private AudioFormat      audioFormat;
	private SourceDataLine   audioLine;
	private boolean          active;
	private boolean          complete;
	private Semaphore        semaphore;
	private SpotifyOggHeader spotifyOggHeader;
	
	/* Self reference. */
	private ChannelPlayer    self;
	
	public ChannelPlayer(Protocol protocol, Track track, byte[] key){
		/* Set self reference. */
		this.self = this;
		
		/* Get AES cipher instance. */
		try {
			this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
		}
		catch (NoSuchAlgorithmException e){
			System.err.println("AES not available! Aargh!");
		}
		catch (NoSuchPaddingException e){
			System.err.println("No padding not available... haha!");
		}
		
		/* Create secret key from bytes. */
		this.key = new SecretKeySpec(key, "AES");
		
		/* Set IV. */
		this.iv = new byte[]{
			(byte)0x72, (byte)0xe0, (byte)0x67, (byte)0xfb,
			(byte)0xdd, (byte)0xcb, (byte)0xcf, (byte)0x77,
			(byte)0xeb, (byte)0xe8, (byte)0xbc, (byte)0x64,
			(byte)0x3f, (byte)0x63, (byte)0x0d, (byte)0x93
		};
		
		/* Initialize cipher with key and iv in encrypt mode. */
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(this.iv));
		}
		catch (InvalidKeyException e){
			System.err.println("Invalid key!");
		}
		catch (InvalidAlgorithmParameterException e){
			System.err.println("Invalid IV!");
		}
		
		/* Set track. */
		this.track = track;
		
		/* Set protocol. */
		this.protocol = protocol;
		
		/* Create piped streams (512 kilobyte buffer). */
		this.output = new PipedOutputStream();
		this.input  = new PipedInputStream(160 * 1024 * 10 / 8);
		
		/* Connect piped streams. */
		try{
			this.output.connect(this.input);
		}
		catch(IOException e){
			System.err.println("Can't connect piped streams!");
		}
		
		/* Set offset and length. */
		this.offset  = 0;
		this.length  = 160 * 1024 * 5 / 8; /* 160 kbit * 5 seconds. */
		this.loading = false;
		
		/* Send first substream request. */
		this.protocol.sendSubstreamRequest(this, this.track, this.offset, this.length);
		
		/* Initialize audio stuff. */
		this.audioStream = null;
		this.audioLine   = null;
		this.active      = false;
		this.complete    = true;
		this.semaphore   = new Semaphore(1);
		
		/* Set to null. */
		this.spotifyOggHeader = null;
		
		/* Acquire permit. Status is paused. */
		this.semaphore.acquireUninterruptibly();
		
		/* Open input stream for playing. */
		if(!this.open(this.input)){
			System.err.println("Can't open input stream for playing!");
		}
	}
	
	public boolean open(final InputStream stream){
		/* Close previously open audio line */
		if(this.check()){
			/* Close player */
			this.close();
		}
		
		/* Read and decode Spotify specific header. */
		try{
			byte[] header = new byte[167];
			
			stream.read(header);
			
			this.spotifyOggHeader = new SpotifyOggHeader(header);
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
			
			this.audioFormat = targetFormat;
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
					
					/* Check if we have enough data. */
					try{
						/* Need 5 seconds audio. */
						int need = (int)(audioFormat.getSampleRate() * audioFormat.getSampleSizeInBits() * 5 / 8);
						
						if(!loading && audioStream.available() < need){
							/* We're loading now. */
							loading = true;
							
							/* Set new offset. */
							offset += length;
							
							/* Send next substream request. */
							protocol.sendSubstreamRequest(self, track, offset, length);
						}
					}
					catch(IOException e){
						/* Ignore */
					}
					
					/* Read data from stream */
					try{
						readBytes = audioStream.read(buffer, 0, buffer.length);
					}
					catch(IOException e){
						e.printStackTrace();
					}
					
					/* Write data to line */
					if(readBytes > 0){
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
	
	/* Start or continue playing. */
	public void play(){
		if(this.check()){
			/* Release permit to resume playing thread. */
			this.semaphore.release();
			
			this.audioLine.start();
		}
	}
	
	/* Stop playing. */
	public void stop(){
		if(this.check()){
			/* Acquire a permit to stop playing thread. */
			this.semaphore.acquireUninterruptibly();
			
			this.audioLine.stop();
		}
	}
	
	/* Get current track. */
	public Track track(){
		return this.track;
	}
	
	/* Get current playback position. */
	public int position(){
		if(this.check()){
			return (int)(this.audioLine.getMicrosecondPosition() / 1000000);
		}
		
		return -1;
	}
	
	/* Get total length in seconds (from spotify ogg header). */
	public int seconds(){
		if(this.spotifyOggHeader != null){
			/* TODO: Remove hard-coded sample rate!? */
			return this.spotifyOggHeader.getSeconds(44100);
		}
		
		return -1;
	}
	
	public boolean isComplete(){
		return this.complete;
	}
	
	/* Close audio line and stream. */
	public void close(){
		if(this.check()){
			this.active = false;
			
			try{
				this.output.close();
			}
			catch(IOException e){
				/* Don't care. */
			}
			
			/* Wait for line to become free */
			while(this.audioLine.isOpen());
		}
	}
	
	/* Check if audio line is open. */
	private boolean check(){
		return this.audioLine != null && this.audioLine.isOpen();
	}
	
	public void channelHeader(Channel channel, byte[] header){
		this.totalLength = 0;
	}
	
	public void channelData(Channel channel, byte[] data){
		/* Offsets needed for deinterleaving. */
		int off, w, x, y, z;
		
		/* Allocate space for ciphertext. */
		byte[] ciphertext = new byte[data.length + 1024];
		byte[] keystream  = new byte[16];
		
		/* Decrypt each 1024 byte block. */
		for(int block = 0; block < data.length / 1024; block++){
			/* Deinterleave the 4x256 byte blocks. */
			off = block * 1024;
			w	= block * 1024 + 0 * 256;
			x	= block * 1024 + 1 * 256;
			y	= block * 1024 + 2 * 256;
			z	= block * 1024 + 3 * 256;
			
			for(int i = 0; i < 1024 && (block * 1024 + i) < data.length; i += 4){
				ciphertext[off++] = data[w++];
				ciphertext[off++] = data[x++];
				ciphertext[off++] = data[y++];
				ciphertext[off++] = data[z++];
			}
			
			/* Decrypt 1024 bytes block. This will fail for the last block. */
			for(int i = 0; i < 1024 && (block * 1024 + i) < data.length; i += 16){
				/* Produce 16 bytes of keystream from the IV. */
				try{
					keystream = this.cipher.doFinal(this.iv);
				}
				catch(IllegalBlockSizeException e){
					e.printStackTrace();
				}
				catch(BadPaddingException e){
					e.printStackTrace();
				}
				
				/* 
				 * Produce plaintext by XORing ciphertext with keystream.
				 * And somehow I also need to XOR with the IV... Please
				 * somebody tell me what I'm doing wrong, or is it the
				 * Java implementation of AES? At least it works like this.
				 */
				for(int j = 0; j < 16; j++){
					ciphertext[block * 1024 + i + j] ^= keystream[j] ^ this.iv[j];
				}

				/* Update IV counter. */
				for(int j = 15; j >= 0; j--){
					this.iv[j] += 1;
					
					if((int)(this.iv[j] & 0xFF) != 0){
						break;
					}
				}
				
				/* Set new IV. */
				try{
					this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(this.iv));
				}
				catch(InvalidKeyException e){
					e.printStackTrace();
				}
				catch(InvalidAlgorithmParameterException e){
					e.printStackTrace();
				}
			}
		}
		
		/* Write data to output stream. */
		try{
			this.output.write(ciphertext, 0, ciphertext.length - 1024);
		}
		catch(IOException e){
			/* Just don't care... */
		}
		
		this.totalLength += data.length;
	}
	
	public void channelEnd(Channel channel){
		Channel.unregister(channel.getId());
		
		/* Loading complete. */
		this.loading = false;
		
		if(this.totalLength < this.length){
			this.audioLine.drain();
			
			this.close();
		}
	}
	
	public void channelError(Channel channel){
		/* Do nothing. */
	}
}
