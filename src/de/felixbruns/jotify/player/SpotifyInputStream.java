package de.felixbruns.jotify.player;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.protocol.channel.ChannelListener;
import de.felixbruns.jotify.util.IntegerUtilities;

public class SpotifyInputStream extends InputStream implements ChannelListener {
	/*
	 * Fixed chunk size of 4096 bytes and
	 * substream size of 5 seconds 160 kbps
	 * audio data.
	 */
	private static final int CHUNK_SIZE     = 4096;
	private static final int SUBSTREAM_SIZE = 160 * 1024 * 5 / 8;
	
	/* 
	 * Protocol, track and file for
	 * substream requesting and handling.
	 */
	private Protocol protocol;
	private Track    track;
	private File     file;
	
	/* 
	 * Map of data chunks which is used as a
	 * sparse buffer.
	 */
	private Map<Integer,byte[]> chunks;
	
	/* Current position in sparse buffer. */
	private int readIndex;
	private int readPosition;
	
	/* Marked position in sparse buffer. */
	private int markIndex;
	private int markPosition;
	
	/* 
	 * Offset in Spotify stream and index
	 * of current chunk in sparse buffer.
	 */
	private int streamLength;
	private int streamOffset;
	private int chunkIndex;
	
	/* 
	 * Cipher implementation, key and IV
	 * for decryption of audio stream.
	 */
	private Cipher cipher;
	private Key    key;
	private byte[] iv;
	
	/* Status flags of this InputStream. */
	private boolean isClosed;
	private boolean isLoading;
	
	/* Lock and Condition for signalling. */
	private Lock      requestLock;
	private Condition requestCondition;
	
	public SpotifyInputStream(Protocol protocol, Track track, int bitrate) throws TimeoutException {
		/* Set Protocol, Track and get File with right bitrate. */
		this.protocol = protocol;
		this.track    = track;
		this.file     = track.getFile(bitrate);
		
		/* Initialize sparse buffer. */
		this.chunks = new HashMap<Integer,byte[]>();
		
		/* Set initial position to zero. */
		this.readIndex    = 0;
		this.readPosition = 0;
		
		/* Set marked position to -1 (not marked yet). */
		this.markIndex    = -1;
		this.markPosition = -1;
		
		/* Set initial stream offset to zero. */
		this.streamLength = -1;
		this.streamOffset =  0;
		this.chunkIndex   =  0;
		
		/* Get AES/CTR/NoPadding instance. */		
		try{
			this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
			this.key    = null;
			this.iv     = null;
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException("AES/CTR is not available!", e);
		}
		catch(NoSuchPaddingException e){
			throw new RuntimeException("NoPadding is not available!", e);
		}
		
		/* 
		 * Request AES key for this track and file
		 * (blocks until key is available or timeout occurs).
		 */
		this.requestKey();
		
		/* Set status flags. */
		this.isClosed  = false;
		this.isLoading = false;
		
		/* Initialize Lock and Condition. */
		this.requestLock      = new ReentrantLock();
		this.requestCondition = this.requestLock.newCondition();
	}
	
	public void debug() throws IOException {
		if(this.streamLength == -1){
			return;
		}
		
		System.out.print("|");
		
		for(int i = 0; i < this.streamLength / CHUNK_SIZE + 1; i++){
			if(this.chunks.containsKey(i)){
				System.out.print("#");
			}
			else{
				System.out.print(" ");
			}
		}
		
		System.out.format("| Available: %5d\n", this.available());
	}
	
	private void requestKey() throws TimeoutException {
		/* Create channel callback. */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send AES key request. */
		try{
			this.protocol.sendAesKeyRequest(callback, this.track, this.file);
		}
		catch(ProtocolException e){
			return;
		}
		
		/* Get AES key. */
		byte[] key = callback.get(1, TimeUnit.SECONDS);
		
		/* Create SecretKeySpec from AES key bytes and set initial IV. */
		this.key = new SecretKeySpec(key, "AES");
		this.iv  = new byte[]{
			(byte)0x72, (byte)0xe0, (byte)0x67, (byte)0xfb,
			(byte)0xdd, (byte)0xcb, (byte)0xcf, (byte)0x77,
			(byte)0xeb, (byte)0xe8, (byte)0xbc, (byte)0x64,
			(byte)0x3f, (byte)0x63, (byte)0x0d, (byte)0x93
		};
		
		/* Initialize cipher with key and IV in encrypt mode. */
		try{
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(this.iv));
		}
		catch(InvalidKeyException e){
			throw new RuntimeException("Invalid key!", e);
		}
		catch(InvalidAlgorithmParameterException e){
			throw new RuntimeException("Invalid IV!", e);
		}
	}
	
	/**
	 * Requests more data from Spotify.
	 * 
	 * @return false if the stream was closed or request offset is out of bounds.
	 */
	private boolean requestData(){
		/* Check if stream is closed. */
		if(this.isClosed){
			return false;
		}
		
		/* Check if we're currently loading data. */
		if(this.isLoading){
			return true;
		}
		
		/* Check if offset is valid. */
		if(this.streamLength != -1 && this.streamOffset >= this.streamLength){
			return false;
		}
		
		/* Set loading flag. */
		this.isLoading = true;
		
		/* Re-Initialize cipher with key and IV in encrypt mode. */
		try{
			/* Create BigInteger from IV. */
			BigInteger counter = new BigInteger(this.iv);
			
			/* Add stream offset divided by 16 (16 byte blocks). */
			counter = counter.add(BigInteger.valueOf(this.streamOffset / 16));
			
			/* Re-Initialize cipher. */
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(counter.toByteArray()));
		}
		catch (InvalidKeyException e){
			throw new RuntimeException("Invalid key!", e);
		}
		catch (InvalidAlgorithmParameterException e){
			throw new RuntimeException("Invalid IV!", e);
		}
		
		/* Request substream, if this fails reset loading flag. */
		try{
			this.protocol.sendSubstreamRequest(this, this.track, this.file, this.streamOffset, SUBSTREAM_SIZE);
		}
		catch(ProtocolException e){
			this.isLoading = false;
			
			return false;
		}
		
		/* Return true to make the thread wait for data. */
		return true;
	}
	
	public int getBitrate(){
		return this.file.getBitrate();
	}
	
	/**
	 * Returns the number of bytes that can be read (or skipped over)
	 * from this input stream without blocking.
	 * 
	 * @return The number of bytes that can be read (or skipped over)
	 *         from this input stream without blocking.
	 * 
	 * @throws IOException If the stream is already closed.
	 */
	public int available() throws IOException {
		/* Check if stream has been closed already. */
		if(this.isClosed){
			throw new IOException("Stream is closed!");
		}
		
		/* Check for end of stream. */
		if(this.streamLength != -1 && this.tell() >= this.streamLength){
			return 0;
		}
		
		/* Check if chunks are available. */
		if(this.chunks.isEmpty() || !this.chunks.containsKey(this.readIndex)){
			return 0;
		}
		
		/* Get current chunk. */
		byte[] chunk = this.chunks.get(this.readIndex);
		
		/* Calculate number of remaining bytes in this chunk. */
		int available = chunk.length - this.readPosition;
		
		/* Add lengths of all directly following chunks. */
		for(int i = (this.readIndex + 1); this.chunks.containsKey(i); i++){
			available += this.chunks.get(i).length;
		}
		
		/* Return number of available bytes. */
		return available;
	}
	
	/**
	 * Closes this input stream and releases any system resources
	 * associated with the stream.
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	public void close() throws IOException {
		this.chunks   = null;
		this.isClosed = true;
	}
	
	/**
	 * Marks the current position in this input stream. A subsequent call to the
	 * reset method repositions this stream at the last marked position so that
	 * subsequent reads re-read the same bytes.
	 * 
	 * @param readlimit This parameter doesn't have any effect.
	 */
	public synchronized void mark(int readlimit){
		this.markIndex    = this.readIndex;
		this.markPosition = this.readPosition;
	}
	
	/**
	 * This method returns {@code true}.
	 * 
	 * @return true
	 */
	public boolean markSupported(){
		return true;
	}
	
	/**
	 * Reads the next byte of data from the input stream. The value byte
	 * is returned as an int in the range 0 to 255. If no byte is available
	 * because the end of the stream has been reached, the value -1 is returned.
	 * This method blocks until input data is available, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return The next byte of data, or -1 if the end of the stream is reached.
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	public int read() throws IOException {
		/* Check for end of stream. */
		if(this.streamLength != -1 && this.tell() >= this.streamLength){
			return -1;
		}
		
		/* Acquire request lock. */
		this.requestLock.lock();
		
		/* Request data until threshold (TODO) is reached. */
		while(this.available() == 0){
			/* Calculate stream offset for next data request. */
			this.streamOffset = this.readIndex * CHUNK_SIZE;
			
			/* Try to request data, if this fails exit loop. */
			if(!this.requestData()){
				break;
			}
			
			/* Wait until a chunk arrived. TODO: timeout here, throw IOException... */
			this.requestCondition.awaitUninterruptibly();
		}
		
		/* Release request lock again. */
		this.requestLock.unlock();
		
		/* Get current chunk. */
		byte[] chunk = this.chunks.get(this.readIndex);
		
		/* Check if chunk is valid. */
		if(chunk == null){
			throw new IOException("Current chunk is null!");
		}
		
		/* Get current byte and increment position. */
		int b = chunk[this.readPosition++] & 0xFF;
		
		/* Increment index if needed. */
		if(this.readPosition >= chunk.length){
			this.readPosition = 0;
			this.readIndex++;
		}
		
		/* Return read byte. */
		return b;
	}
	
	/**
	 * Reads up to {@code len} bytes of data from the input stream into an array
	 * of bytes. An attempt is made to read as many as {@code len} bytes, but a
	 * smaller number may be read. The number of bytes actually read is returned
	 * as an integer.
	 * 
	 * This method blocks until input data is available, end of stream is detected,
	 * or an exception is thrown.
	 * 
	 * If {@code len} is zero, then no bytes are read and 0 is returned; otherwise,
	 * there is an attempt to read at least one byte. If no byte is available because
	 * the stream is at end of stream, the value -1 is returned; otherwise, at least
	 * one byte is read and stored into {@code len}.
	 * 
	 * The first byte read is stored into element {@code b[off]}, the next one into
	 * {@code b[off+1]}, and so on. The number of bytes read is, at most, equal to
	 * {@code len}. Let k be the number of bytes actually read; these bytes will be
	 * stored in elements {@code b[off]} through {@code b[off+k-1]}, leaving elements
	 * {@code b[off+k]} through {@code b[off+len-1]} unaffected.
	 * 
	 * In every case, elements {@code b[0]} through {@code b[off]} and elements
	 * {@code b[off+len]} through {@code b[b.length-1]} are unaffected.
	 * 
	 * @param b   The buffer into which the data is read.
	 * @param off The start offset in array b at which the data is written.
	 * @param len The maximum number of bytes to read.
	 * 
	 * @return The total number of bytes read into the buffer, or -1 if there is no
	 *         more data because the end of the stream has been reached.
	 * 
	 * @throws IOException If the first byte cannot be read for any reason other than
	 *         end of stream, or if the input stream has been closed, or if some other
	 *         I/O error occurs.
	 * @throws NullPointerException If {@code b} is null.
	 * @throws IndexOutOfBoundsException If {@code off} is negative, {@code len} is
	 *         negative, or {@code len} is greater than {@code b.length - off}
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		/* Check offset and length arguments. */
		if(off < 0 || len < 0 || len > b.length - off){
			throw new IndexOutOfBoundsException();
		}
		
		/* Number of bytes actually copied. */
		int read = 0;
		
		/* Check for end of stream. */
		if(this.streamLength != -1 && this.tell() >= this.streamLength){
			return -1;
		}
		
		/* Acquire request lock. */
		this.requestLock.lock();
		
		/* Request data until threshold (TODO) is reached. */
		while(this.available() == 0){
			/* Calculate stream offset for next data request. TODO: search next hole. */
			//if(this.readPosition > 0){
			//	this.streamOffset = (this.readIndex + 1) * CHUNK_SIZE;
			//}
			//else{
				this.streamOffset = this.readIndex * CHUNK_SIZE;
			//}
			
			/* Try to request data, if this fails exit loop. */
			if(!this.requestData()){
				break;
			}
			
			/* Wait until a chunk arrived. */
			this.requestCondition.awaitUninterruptibly();
		}
		
		/* Release request lock again. */
		this.requestLock.unlock();
		
		/* Determine number of bytes to copy. */
		len = Math.min(len, this.available());
		
		/* Copy bytes in steps until finished. */
		while(read < len){
			/* Get current chunk. */
			byte[] chunk = this.chunks.get(this.readIndex);
			
			/* Check if chunk is valid. */
			if(chunk == null){
				System.out.println(this.chunks);
				
				throw new IOException("'chunk' is null!");
			}
			
			/* Calculate number of bytes to copy in this step. */
			int num = Math.min(chunk.length - this.readPosition, len - read);
			
			/* Copy bytes into array. */
			System.arraycopy(chunk, this.readPosition, b, off, num);
			
			/* Increment offset and number of read bytes for next step. */
			off  += num;
			read += num;
			
			/* Increment position for next step. */
			this.readPosition += num;
			
			/* Increment index if needed. */
			if(this.readPosition >= CHUNK_SIZE){
				this.readPosition = 0;
				this.readIndex++;
			}
			
			/* Check for end of stream. */
			if(this.streamLength != -1 && this.tell() >= this.streamLength){
				break;
			}
		}
		
		//this.debug();
		
		/* Return number of actually read bytes. */
		return read;
	}
	
	/**
	 * TODO
	 */
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	/**
	 * Repositions this stream to the position at the time the {@code mark} method
	 * was last called on this input stream.
	 * 
	 * If the method {@code mark} has not been called since the stream was created,
	 * then an IOException is thrown. If such an {@link IOException} is not thrown,
	 * then the stream is reset to a state such that all the bytes read since the
	 * most recent call to {@code mark} will be resupplied to subsequent callers of
	 * the {@code read} method, followed by any bytes that otherwise would have been
	 * the next input data as of the time of the call to {@code reset}.
	 * 
	 * @throws IOException If this stream has not been marked.
	 */
	public synchronized void reset() throws IOException {
		/* Check if stream is closed. */
		if(this.isClosed){
			throw new IOException("Stream is closed!");
		}
		
		/* Check if mark has been called sometime before. */
		if(this.markIndex == -1 || this.markPosition == -1){
			throw new IOException("No call to 'mark' was made yet!");
		}
		
		/* Reset to marked position. */
		this.readIndex    = this.markIndex;
		this.readPosition = this.markPosition;
	}
	
	/**
	 * Seeks to the offset {@code off} in this stream. 
	 * 
	 * @param p The offset to seek to in bytes.
	 * 
	 * @throws IOException If the seek offset is invalid or the stream is closed.
	 */
	public void seek(long off) throws IOException {
		/* Check if stream is closed. */
		if(this.isClosed){
			throw new IOException("Stream is closed!");
		}
		
		/* Check if the seek offset is valid. */
		if(off < 0 || off > this.streamLength){
			throw new IOException("Invalid seek offset!");
		}
		
		/* Seek to requested offset. */
		this.readIndex    = (int)(off / CHUNK_SIZE);
		this.readPosition = (int)(off % CHUNK_SIZE);
	}
	
	/**
	 * Returns the current stream position.
	 * 
	 * @return The current stream position.
	 */
	public int tell(){
		return this.readIndex * CHUNK_SIZE + this.readPosition;
	}
	
	/**
	 * Skips over and discards {@code n} bytes of data from this input stream.
	 * The skip method may, for a variety of reasons, end up skipping over some
	 * smaller number of bytes, possibly 0. This may result from any of a number
	 * of conditions; reaching end of stream before {@code n} bytes have been
	 * skipped is only one possibility. The actual number of bytes skipped is
	 * returned. If {@code n} is negative, no bytes are skipped.
	 * 
	 * @param n The number of bytes to be skipped.
	 * 
	 * @return The actual number of bytes skipped. 
	 */
	public long skip(long n) throws IOException {
		/* Check argument. */
		if(n < 0){
			return 0;
		}
		
		/* Calculate number of bytes actually skipped. */
		n = Math.min(n, this.streamLength - this.tell());
		
		/* Skip bytes. */
		this.readIndex    += (int)(n / CHUNK_SIZE);
		this.readPosition += (int)(n % CHUNK_SIZE);
		
		/* Return number of bytes skipped. */
		return n;
	}
	
	public void channelHeader(Channel channel, byte[] header){
		/* Calculate chunk index in sparse buffer. */
		this.chunkIndex = this.streamOffset / CHUNK_SIZE;
		
		/* Get stream length. */
		if(header[0] == 0x03){
			this.streamLength = IntegerUtilities.bytesToInteger(header, 1) << 2;
		}
	}
	
	public void channelData(Channel channel, byte[] data){
		/* Offsets needed for deinterleaving. */
		int off, w, x, y, z;
		
		/* Allocate space for ciphertext. */
		byte[] ciphertext = new byte[data.length];
		
		/* Deinterleave 4x256 byte blocks. */
		for(int block = 0; block < data.length / 1024; block++){
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
		}
		
		/* Decrypt data. */
		byte[] plaintext = this.cipher.update(ciphertext);
		
		/* Put decrypted data into sparse buffer. */
		this.chunks.put(this.chunkIndex++, plaintext);
		
		/* Signal data arrival. */
		this.requestLock.lock();
		this.requestCondition.signal();
		this.requestLock.unlock();
	}
	
	public void channelEnd(Channel channel){
		/* Unregister finished channel. */
		Channel.unregister(channel.getId());
		
		/* Reset chunk index and status flags. */
		this.chunkIndex = 0;
		this.isLoading  = false;
		
		/* Signal end. */
		this.requestLock.lock();
		this.requestCondition.signal();
		this.requestLock.unlock();
	}
	
	public void channelError(Channel channel){
		/* Just ignore channel errors. */
	}
}
