package de.felixbruns.spotify.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class GZIP {
	public static byte[] inflate(byte[] bytes){
		ByteArrayInputStream byteArrayInputStream;
		GZIPInputStream      gzipInputStream;
		Buffer               buffer;
		
		/* Get InputStream of bytes. */
		byteArrayInputStream = new ByteArrayInputStream(bytes);
		
		/* Allocate buffer. */
		buffer = new Buffer();
		
		/* Inflate deflated data. */
		try{
			gzipInputStream = new GZIPInputStream(byteArrayInputStream);
			
			while(gzipInputStream.available() > 0){
				buffer.appendByte((byte)gzipInputStream.read());
			}
		}
		catch(IOException e){
			/* 
			 * This also catches EOFException's.
			 * Do nothing, just return what we
			 * decompressed so far.
			 */
		}
		
		return buffer.getBytes();
	}
}
