package de.felixbruns.jotify.player;

/*
 * Copyright &copy; Jon Kristensen, 2008.
 * Copyright &copy; Felix Bruns, 2010.
 * All rights reserved.
 * 
 * This is version 1.1 of this source code, made to work with JOrbis 1.x. The
 * last time this file was updated was the 11th of April, 2010.
 * 
 * Version history:
 * 
 * 1.0: Initial release.
 * 1.1: Modified by Felix Bruns for use in jotify.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *   * Neither the name of jonkri.com nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.*;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;

import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.util.MathUtilities;

public class SpotifyOggPlayer implements Runnable, Player {
	/* Information on the track. */
	private Protocol protocol;
	private Track    track;
	private int      bitrate;
	
	/* Input stream and OGG header. */
	private SpotifyInputStream input;
	private SpotifyOggHeader   header;
	
	/* Audio output. */
	private SourceDataLine output;
	
	/* A playback listener we can notify. */
	private PlaybackListener listener;
	
	/* Semaphore for pausing the player. */
	private Semaphore pause;
	
	/* Flags, position and current packet. */
	private boolean isInitialized;
	private boolean needsMoreData;
	private boolean isClosed;
	private int     position;
	private int     dataPosition;
	private int     packet;
	
	/*
	 * We need a buffer, it's size, a count to know how many bytes we have read
	 * and an index to keep track of where we are.
	 */
	private byte[] buffer;
	private int    bufferSize;
	private int    count;
	private int    index;
	
	/*
	 * JOgg and JOrbis require fields for the converted buffer. This is a buffer
	 * that is modified in regards to the number of audio channels. Naturally,
	 * it will also need a size.
	 */
	private byte[] convertedBuffer;
	private int    convertedBufferSize;
	
	/* 
	 * A three-dimensional an array with PCM information and the index for the
	 * PCM information.
	 */
	private float[][][] pcmInfo;
	private int[]       pcmIndex;
	
	/* The four required JOgg objects. */
	private Packet      joggPacket;
	private Page        joggPage;
	private StreamState joggStreamState;
	private SyncState   joggSyncState;
	
	/* The four required JOrbis objects. */
	private DspState    jorbisDspState;
	private Block       jorbisBlock;
	private Comment     jorbisComment;
	private Info        jorbisInfo;
	
	/**
	 * Creates a new {@link SpotifyOggPlayer} using the given {@link Protocol}
	 * and {@link Track}. The {@code bitrate} argument specifies a preferred
	 * bitrate to use. A {@link PlaybackListener} can be supplied or can be set
	 * to {@code null}.
	 * 
	 * @param protocol The {@link Protocol} instance to use for requesting substreams.
	 */
	public SpotifyOggPlayer(Protocol protocol){
		/* Set protocol. */
		this.protocol = protocol;
		
		/* Set status. */
		this.isInitialized = false;
	}
	
	/**
	 * Play a track.
	 * 
	 * @param track    The {@link Track} to play.
	 * @param bitrate  The bitrate to prefer when choosing a {@link File} to play.
	 * @param listener The {@link PlaybackListener} to use or {@code null}.
	 * 
	 * @throws TimeoutException         If a timeout occurs requesting the AES key for
	 *                                  this {@link Track}.
	 * @throws IOException              If there is an error with any I/O operation.
	 * @throws LineUnavailableException If the audio line is not available for playback.
	 */
	public void play(Track track, int bitrate, PlaybackListener listener) throws TimeoutException, IOException, LineUnavailableException {
		/* Check if player was stopped before. */
		if(this.isInitialized){
			throw new IllegalStateException("Player needs to be stopped before calling this method again.");
		}
		
		/* Set track and bitrate. */
		this.track   = track;
		this.bitrate = bitrate;
		
		/* Get input stream for track and preferred bitrate. */
		this.input = new SpotifyInputStream(this.protocol, track, bitrate);
		
		/* Get actual bitrate of stream. */
		this.bitrate = this.input.getBitrate();
		
		/* Read Spotify specific OGG header from stream. */
		this.buffer = new byte[167];
		
		this.input.read(this.buffer);
		
		this.header = SpotifyOggHeader.decode(this.buffer);
		
		/* Set playback listener. */
		this.listener = listener;
		
		/* Semaphore for pausing the player. */
		this.pause = new Semaphore(1);
		
		/* Set initial values of flags and variables. */
		this.needsMoreData = true;
		this.isClosed      = false;
		this.position      = 0;
		
		/* Create JOgg and JOrbis objects. */
		this.joggPacket      = new Packet();
		this.joggPage        = new Page();
		this.joggStreamState = new StreamState();
		this.joggSyncState   = new SyncState();
		this.jorbisDspState  = new DspState();
		this.jorbisBlock     = new Block(this.jorbisDspState);
		this.jorbisComment   = new Comment();
		this.jorbisInfo      = new Info();
		
		/* Set up buffer for JOrbis. */
		this.buffer     = null;
		this.bufferSize = 2048;
		this.count      = 0;
		this.index      = 0;
		this.packet     = 1;
		
		/* Initialize JOrbis. */
		this.joggSyncState.init();
		this.joggSyncState.buffer(this.bufferSize);
		
		/*
		 * Fill the buffer with the data from SyncState's internal buffer.
		 * Note how the size of this new buffer is different from bufferSize.
		 */
		this.buffer = this.joggSyncState.data;
		
		/* Read OGG Vorbis header. */
		this.readOggVorbisHeader();
		
		/* Remember start of data. */
		this.dataPosition = this.input.tell();
		
		/* This buffer is used by the decoding method. */
		this.convertedBufferSize = this.bufferSize * 2;
		this.convertedBuffer     = new byte[this.convertedBufferSize];
		
		/* Initializes the DSP synthesis. */
		this.jorbisDspState.synthesis_init(this.jorbisInfo);
		
		/* Make the Block object aware of the DSP. */
		this.jorbisBlock.init(this.jorbisDspState);
		
		/*
		 * We create the PCM variables. The index is an array with the same
		 * length as the number of audio channels.
		 */
		this.pcmInfo  = new float[1][][];
		this.pcmIndex = new int[this.jorbisInfo.channels];
		
		/* Initialize SourceDataLine. */
		AudioFormat   format = new AudioFormat(this.jorbisInfo.rate, 16, this.jorbisInfo.channels, true, false);
		DataLine.Info info   = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
		
		/* Check if the line is supported. */
		if(!AudioSystem.isLineSupported(info)){
			throw new LineUnavailableException("Audio output line is not supported!");
		}
		
		/*
		 * Everything seems to be alright. Let's try to open a line with the
		 * specified format and start the source data line.
		 */
		try{
			this.output = (SourceDataLine)AudioSystem.getLine(info);
			
			/* Open and start audio line. */
			this.output.open(format);
			this.output.start();
		}
		catch(LineUnavailableException e){
			throw new LineUnavailableException("The audio output line could not be opened due to resource restrictions.");
		}
		catch(IllegalStateException e){
			throw new LineUnavailableException("The audio output line is already open.");
		}
		catch(SecurityException e){
			throw new LineUnavailableException("The audio output line could not be opened due to security restrictions.");
		}
		
		/* Set status. */
		this.isInitialized = true;
		
		/* Start playback thread. */
		new Thread(this).start();
	}
	
	/**
	 * Starts or resumes playback.
	 * 
	 * @throws IllegalStateException If the player is not initialized yet.
	 */
	public void play(){
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Stop audio line. */
		this.output.start();
		
		this.pause.release();
	}
	
	/**
	 * Pauses playback.
	 * 
	 * @throws IllegalStateException If the player is not initialized yet.
	 */
	public void pause(){
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Stop audio line. */
		this.output.stop();
		
		/* Acquire pause permit. */
		this.pause.acquireUninterruptibly();
	}
	
	/**
	 * Stops playback and closes the player.
	 */
	public void stop(){
		/* Set closed flag. */
		this.isClosed = true;
		
		/* Close audio output line. */
		if(this.output != null){
			this.output.close();
		}
		
		/* Reset to uninitialized state. */
		this.isInitialized = false;
	}
	
	/**
	 * Returns the length of the track in milliseconds.
	 * 
	 * @return The length of the track in milliseconds.
	 */
	public int length(){
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Get length of track from Spotify specific OGG header. */
		return this.header.getLength(this.jorbisInfo.rate);
	}
	
	/**
	 * Returns the current playback position in milliseconds.
	 * 
	 * @return The current playback position in milliseconds.
	 */
	public int position(){
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Return current position. */
		return this.position;
	}
	
	/**
	 * Seeks to a given position in milliseconds.
	 * 
	 * @param ms The position in milliseconds to seek to.
	 * 
	 * @throws IOException If an I/O error occurs while seeking.
	 */
	public void seek(int ms) throws IOException {
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Acuire pause permit first when seeking. */
		this.pause.acquireUninterruptibly();
		
		/* Estimate seek offset in bytes from given milliseconds. */
		int off = this.bitrate / 1000 * ms / 8;
		
		/* If offset is too small, seek to start of data. */
		if(off < this.dataPosition){
			this.input.seek(this.dataPosition);
			
			/* Reset JOgg sync state. */
			this.joggSyncState.reset();
			
			/* Release pause permit again. */
			this.pause.release();
			
			return;
		}
		/* Seek to estimated offset. */
		else{
			this.input.seek(off);
		}
		
		/* Reset JOgg sync state. */
		this.joggSyncState.reset();
		
		/* Previous difference. */
		int prev = -1;
		
		/*
		 * Adjust seek offset until position matches.
		 * 
		 * TODO: bisection search.
		 */
		while(true){
			/* Read a page. */
			if(this.joggSyncState.pageout(this.joggPage) > 0){
				/* Get actual millisecond position from page. */
				int actual = (int)(this.joggPage.granulepos() / (this.jorbisInfo.rate / 1000));
				
				/* Calculate difference. */
				int diff = ms - actual;
				
				/* If difference is smaller than 10 ms or doesn't change anymore, we're finsihed. */
				if(Math.abs(diff) < 10 || Math.abs(diff) == prev){
					this.position = actual;
					
					break;
				}
				/* If difference is bigger, seek by this difference. */
				else{
					off += this.bitrate / 1000 * diff / 8;
				}
				
				/* Set previous difference. */
				prev = Math.abs(diff);
				
				/* Seek to new offset. */
				this.input.seek(off);
				
				/* Reset JOgg sync state. */
				this.joggSyncState.reset();
			}
			else{
				/* Read more data from input stream. */
				this.getMoreDataIfNecessary();
			}
		}
		
		/* Release pause permit again. */
		this.pause.release();
	}
	
	/**
	 * Returns the current volume of the audio line.
	 * 
	 * @return The volume as a value between 0.0 and 1.0.
	 */
	public float volume(){
		float gain;
		float volume;
		
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Get gain control. */
		FloatControl control = (FloatControl)this.output.getControl(FloatControl.Type.MASTER_GAIN);
		
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
	 * Sets a new volume on the audio line.
	 * 
	 * @param volume A value between 0.0 and 1.0.
	 * 
	 * @throws IllegalArgumentException If the volume value is invalid.
	 */
	public void volume(float volume){
		float gain;
		
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Check arguments. */
		if(volume < 0.0f || volume > 1.0f){
			throw new IllegalArgumentException("Volume has to be a value from 0.0 to 1.0!");
		}
		
		/* Get gain control. */
		FloatControl control = (FloatControl)this.output.getControl(FloatControl.Type.MASTER_GAIN);
		
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
	
	/* Playback thread. Processes OGG Vorbis pages until finished. */
	public void run(){
		/* Check if player is initialized. */
		if(!this.isInitialized){
			throw new IllegalStateException("No call to play(Track, int, PlaybackListener) has been made yet.");
		}
		
		/* Fire playback started event. */
		if(this.listener != null){
			this.listener.playbackStarted(this.track);
		};
		
		/* We need more data... */
		this.needsMoreData = true;
		
		/* Process audio packets. */
		try{
			while(!this.isClosed && this.needsMoreData){
				/* Wait if we're paused. Release permit right away. */
				this.pause.acquireUninterruptibly();
				this.pause.release();
				
				/* Process next OGG Vorbis page. */
				this.processOggVorbisPage();
				
				/*  Set current position and fire playback position event. */
				int position = (int)(this.joggPage.granulepos() / (this.jorbisInfo.rate / 1000));
				
				if(position > 0){
					this.position = position;
					
					if(this.listener != null){
						this.listener.playbackPosition(this.track, position);
					}
				}
				
				/* Read more data from stream. */
				this.getMoreDataIfNecessary();
			}
		}
		catch(IOException e){
			/* There was some decoding error... */
			e.printStackTrace();
		}
		
		/* Fire playback finished event. */
		if(!this.isClosed && this.listener != null){
			this.listener.playbackFinished(this.track);
		};
		
		/* Clean up. */
		this.joggStreamState.clear();
		this.jorbisBlock.clear();
		this.jorbisDspState.clear();
		this.jorbisInfo.clear();
		this.joggSyncState.clear();
		
		/* Close input stream and audio line. */
		try{
			this.input.close();
			this.output.close();
		}
		catch(IOException e){
			/* Ignore. */
		}
	}
	
	private void readOggVorbisHeader() throws IOException {
		while(this.needsMoreData){
			this.readDataFromInputStream();
			
			this.joggSyncState.wrote(this.count);
			
			/* Read packets. */
			switch(this.packet){
				case 1: {
					this.readFirstPacket();
					
					break;
				}
				case 2:
				case 3: {
					this.readSecondAndThirdPacket();
				}
			}
			
			/* Check state. */
			if(this.packet == 4){
				this.needsMoreData = false;
			}
			
			this.index  = this.joggSyncState.buffer(this.bufferSize);
			this.buffer = this.joggSyncState.data;
			
			/* Check if we didn't get enough data. */
			if(this.count == 0 && this.needsMoreData){
				throw new IOException("Not enough header data was supplied.");
			}
		}
	}
	
	private void readFirstPacket() throws IOException {
		switch(this.joggSyncState.pageout(this.joggPage)){
			/* If there is a hole in the data, we must exit. */
			case -1: {
				throw new IOException("There is a hole in the first packet data.");
			}
			/* If we need more data, we break to get it. */
			case 0: {
				break;
			}
			/* Get packet data. */
			case 1: {
				this.joggStreamState.init(this.joggPage.serialno());
				this.joggStreamState.reset();
				
				this.jorbisInfo.init();
				this.jorbisComment.init();
				
				/* Get first header page. */
				if(this.joggStreamState.pagein(this.joggPage) == -1){
					throw new IOException("Error reading first header page.");
				}
				
				/* Get first header packet. */
				if(this.joggStreamState.packetout(this.joggPacket) != 1){
					throw new IOException("Error reading first header packet.");
				}
				
				/* Interpret first header packet. */
				if(this.jorbisInfo.synthesis_headerin(this.jorbisComment, this.joggPacket) < 0){
					throw new IOException("Error interpreting first packet. Apparantly, it's not Vorbis data.");
				}
				
				/* We're done here, let's increment 'packet'. */
				this.packet++;
				
				break;
			}
		}
	}
	
	private void readSecondAndThirdPacket() throws IOException {
		switch(this.joggSyncState.pageout(this.joggPage)){
			/* If there is a hole in the data, we must exit. */
			case -1: {
				throw new IOException("There is a hole in the second or third packet data.");
			}
			/* If we need more data, we break to get it. */
			case 0: {
				break;
			}
			/* Get packet data. */
			case 1: {
				this.joggStreamState.pagein(this.joggPage);

				switch(this.joggStreamState.packetout(this.joggPacket)){
					/* If there is a hole in the data, we must exit. */
					case -1: {
						throw new IOException("There is a hole in the first packet data.");
					}
					/* If we need more data, we break to get it. */
					case 0: {
						break;
					}
					/* We got a packet, let's process it. */
					case 1: {
						this.jorbisInfo.synthesis_headerin(this.jorbisComment, this.joggPacket);
						
						/* We're done here, let's increment "packet". */
						this.packet++;
						
						break;
					}
				}
				
				break;
			}
		}
	}

	private void readDataFromInputStream() throws IOException {
		try{
			if(this.index < 0){
				this.index = 0;
			}
			
			this.count = this.input.read(this.buffer, this.index, this.bufferSize);
		}
		catch(IOException e){
			throw new IOException("Couldn't read from input stream.", e);
		}
	}
	
	private void getMoreDataIfNecessary() throws IOException {
		/* If we need more data... */
		if(this.needsMoreData){
			/* We get the new index and an updated buffer. */
			this.index  = this.joggSyncState.buffer(this.bufferSize);
			this.buffer = this.joggSyncState.data;
			
			/* Read from InputStream. */
			this.readDataFromInputStream();
			
			/* We let SyncState know how many bytes we read. */
			this.joggSyncState.wrote(this.count);
			
			/* There's no more data in the stream. */
			if(this.count == 0 || this.count == -1){
				this.needsMoreData = false;
			}
		}
	}
	
	private void processOggVorbisPage() throws IOException {
		switch(this.joggSyncState.pageout(this.joggPage)){
			case -1: {
				/* There is a hole in the data. We proceed. */
				break;
			}
			/* If we need more data, we break to get it. */
			case 0: {
				break;
			}
			/* If we have successfully checked out a page, we continue. */
			case 1: {
				this.joggStreamState.pagein(this.joggPage);
				
				/* If granulepos() returns 0, we don't need more data. */
				if(this.joggPage.granulepos() == 0){
					this.needsMoreData = false;
					
					break;
				}
				
				this.processOggVorbisPackets();
				
				/* If the page is the end-of-stream. */
				if(this.joggPage.eos() != 0){
					this.needsMoreData = false;	
				}
			}
		}
	}
	
	private void processOggVorbisPackets(){
		while(true){
			switch(this.joggStreamState.packetout(this.joggPacket)){
				case -1: {
					/* There is a hole in the data, we continue though. */
				}
				/* If we need more data, we break to get it. */
				case 0:
					return;
				case 1:
					this.decodeCurrentPacket();
			}
		}
	}
	
	/* Decodes the current packet and sends it to the audio output line. */
	private void decodeCurrentPacket(){
		int samples;
		
		/* Check that the packet is a audio data packet etc. */
		if(this.jorbisBlock.synthesis(this.joggPacket) == 0){
			/* Give the block to the DspState object. */
			this.jorbisDspState.synthesis_blockin(this.jorbisBlock);
		}
		
		/* We need to know how many samples to process. */
		int range;

		/*
		 * Get the PCM information and count the samples. And while these
		 * samples are more than zero...
		 */
		while((samples = this.jorbisDspState.synthesis_pcmout(this.pcmInfo, this.pcmIndex)) > 0){
			/* We need to know for how many samples we are going to process. */
			if(samples < this.convertedBufferSize){
				range = samples;
			}
			else{
				range = this.convertedBufferSize;
			}

			/* For each channel... */
			for(int i = 0; i < this.jorbisInfo.channels; i++){
				int sampleIndex = i * 2;

				/* For every sample in our range... */
				for(int j = 0; j < range; j++){
					/*
					 * Get the PCM value for the channel at the correct
					 * position.
					 */
					int value = (int) (this.pcmInfo[0][i][this.pcmIndex[i] + j] * 32767);
					
					/*
					 * We make sure our value doesn't exceed or falls below
					 * +-32767.
					 */
					if(value > 32767){
						value = 32767;
					}
					if(value < -32768){
						value = -32768;
					}
					
					/*
					 * It the value is less than zero, we bitwise-or it with
					 * 32768 (which is 1000000000000000 = 10^15).
					 */
					if(value < 0) value = value | 32768;
					
					/*
					 * Take our value and split it into two, one with the last
					 * byte and one with the first byte.
					 */
					this.convertedBuffer[sampleIndex]     = (byte)(value);
					this.convertedBuffer[sampleIndex + 1] = (byte)(value >>> 8);
					
					/*
					 * Move the sample index forward by two (since that's how
					 * many values we get at once) times the number of channels.
					 */
					sampleIndex += 2 * (this.jorbisInfo.channels);
				}
			}
			
			int offset = 0;
			int length = 2 * this.jorbisInfo.channels * range;
			
			/* Write data to SourceDataLine. */
			this.output.write(this.convertedBuffer, offset, length);
			
			/* Update the DspState object. */
			this.jorbisDspState.synthesis_read(range);
		}
	}
}
