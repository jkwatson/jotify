package de.felixbruns.jotify.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Class providing convenience methods for handling GZIP compressed data.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class GZIP {
	/**
	 * Buffer size to use when inflating data.
	 */
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * Inflate a buffer of GZIP compressed data.
	 * 
	 * @param bytes A buffer containing GZIP compressed data.
	 * 
	 * @return A buffer containing uncompressed data.
	 */
	public static byte[] inflate(byte[] bytes){
		ByteArrayInputStream byteArrayInputStream;
		GZIPInputStream      gzipInputStream;
		List<ByteBuffer>     buffers;
		ByteBuffer           buffer;
		int                  nbytes;
		
		/* Get InputStream of bytes. */
		byteArrayInputStream = new ByteArrayInputStream(bytes);
		
		/* Allocate buffer(s). */
		buffer  = ByteBuffer.allocate(GZIP.BUFFER_SIZE);
		buffers = new LinkedList<ByteBuffer>();
		nbytes  = 0;
		
		/* Inflate deflated data. */
		try{
			gzipInputStream = new GZIPInputStream(byteArrayInputStream);
			
			while(gzipInputStream.available() > 0){
				if(!buffer.hasRemaining()){
					nbytes += buffer.position();
					
					buffer.flip();
					buffers.add(buffer);
					
					buffer = ByteBuffer.allocate(GZIP.BUFFER_SIZE);
				}
				
				buffer.put((byte)gzipInputStream.read());
			}
		}
		catch(IOException e){
			/* 
			 * This also catches EOFException's. Do nothing
			 * and just return what we decompressed so far.
			 */
		}
		
		/* Create final data buffer. */
		byte[]     data       = new byte[nbytes + buffer.position()];
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		
		buffer.flip();
		buffers.add(buffer);
		
		/* Combine buffers into final buffer. */
		for(ByteBuffer b : buffers){
			dataBuffer.put(b);
		}
		
		return data;
	}
}
