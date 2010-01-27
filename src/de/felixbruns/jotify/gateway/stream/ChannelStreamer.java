package de.felixbruns.jotify.gateway.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sun.net.httpserver.HttpExchange;

import de.felixbruns.jotify.cache.SubstreamCache;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.SpotifyOggHeader;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;

public class ChannelStreamer implements ChannelListener {
	/* Decryption stuff. */
	private Cipher cipher;
	private Key    key;
	private byte[] iv;
	
	/* Requesting and loading stuff. */
	private Track            track;
	private Protocol         protocol;
	private int              channelOffset;
	private int              channelLength;
	private int              channelTotal;
	private SpotifyOggHeader header;
	private HttpExchange     exchange;
	private OutputStream     output;
	
	/* Caching of substreams. */
	private SubstreamCache cache;
	private byte[]         cacheData;
	
	private int total = 0;
	
	public ChannelStreamer(Protocol protocol, Track track, byte[] key, HttpExchange exchange){
		/* Set output stream and cache. */
		this.exchange = exchange;
		this.output   = exchange.getResponseBody();
		this.cache    = new SubstreamCache();
		
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
		
		/* Set offset and length. */
		this.channelOffset = 0;
		this.channelLength = 160 * 1024 * 5 / 8; /* 160 kbit * 5 seconds. */
		
		/* Header and semaphore to wait on. */
		this.header = null;
		
		/* Send first substream request. */
		String hash = this.cache.hash(this.track, this.channelOffset, this.channelLength);
		
		if(this.cache != null && this.cache.contains("substream", hash)){
			this.cache.load("substream", hash, this);
		}
		else{
			try{
				this.protocol.sendSubstreamRequest(this, this.track, this.channelOffset, this.channelLength);
			}
			catch(ProtocolException e){
				return;
			}
		}
	}
	
	public void channelHeader(Channel channel, byte[] header){
		this.cacheData = new byte[this.channelLength];
		
		this.channelTotal = 0;
	}
	
	public void channelData(Channel channel, byte[] data){
		/* Offsets needed for deinterleaving. */
		int off, w, x, y, z;
		
		/* Copy data to cache buffer. */
		for(int i = 0; i < data.length; i++){
			this.cacheData[this.channelTotal + i] = data[i];
		}
		
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
		
		/* Save data to output stream. */
		try{
			off = 0;
			
			/* Check if we decoded the header yet. */
			if(this.header == null){
				/* Get header from data. */
				byte[] bytes = Arrays.copyOfRange(ciphertext, 0, 167);
				
				/* Decode header. */
				this.header = new SpotifyOggHeader(bytes);
				
				/* Send response headers. */
				System.out.format("Header: 0x%08x\n", (this.header.getLength() & 0xfffff000) - 167);
				
				this.exchange.sendResponseHeaders(200, (this.header.getLength() & 0xfffff000) - 167);
				
				off = 167;
			}
			
			this.output.write(ciphertext, off, data.length - off);
			this.output.flush();
			
			/* 
			 * Don't subtract 'off' here! Otherwise we would
			 * accidentially close the stream in channelEnd!
			 */
			this.channelTotal += data.length;
			
			this.total += data.length;
		}
		catch(Exception e){
			/* Don't care. */
		}
	}
	
	public void channelEnd(Channel channel){
		/* Create cache hash. */
		String hash = this.cache.hash(this.track, this.channelOffset, this.channelLength);
		
		/* Save to cache. */
		if(this.cache != null && !this.cache.contains("substream", hash)){
			this.cache.store("substream", hash, this.cacheData, this.channelTotal);
		}
		
		/* Send next substream request. */
		try{
			if(this.channelTotal < this.channelLength){
				this.output.close();
				
				System.out.format("Stream: 0x%08x\n", this.total - 167);
				
				return;
			}
			
			this.channelOffset += this.channelLength;
			
			hash = this.cache.hash(this.track, this.channelOffset, this.channelLength);
			
			if(this.cache != null && this.cache.contains("substream", hash)){
				this.cache.load("substream", hash, this);
			}
			else{
				this.protocol.sendSubstreamRequest(this, this.track, this.channelOffset, this.channelLength);
			}
		}
		catch(IOException e){
			/* Ignore. */
		}
		catch(ProtocolException e){
			/* Ignore. */
		}
		
		Channel.unregister(channel.getId());
	}
	
	public void channelError(Channel channel){
		/* Ignore. */
	}
}
