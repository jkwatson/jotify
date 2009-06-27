package de.felixbruns.jotify.gateway.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.http.DeinterleavingInputStream;
import de.felixbruns.jotify.player.http.DecryptingInputStream;

public class HTTPStreamer implements Runnable {
	/* 
	 * Protocol, Track object and variables for
	 * substream requesting and handling.
	 */
	private InputStream  input;
	private OutputStream output;
	
	public HTTPStreamer(List<String> urls, Track track, byte[] key, OutputStream output){
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
		
		/* Get input and output streams. */
		this.output = output;
		this.input  = new BufferedInputStream(
			new DecryptingInputStream(
				new DeinterleavingInputStream(streams),
				key
			)
		);
		
		/* Start thread which writes data to the output */
		new Thread(this, "HTTPStreamer-Thread").start();
	}
	
	public void run(){
		/* Buffer for data and number of bytes read */
		byte[] buffer = new byte[1024];
		int read = 0;
		
		/* Read-write loop. */
		try{
			this.input.skip(167);
			
			while(read != -1){
				/* Read data from input stream and write it to the output stream. */
				if((read = this.input.read(buffer, 0, buffer.length)) > 0){
					this.output.write(buffer, 0, read);
				}
			}
			
			/* Close streams. */
			this.input.close();
			this.output.close();
		}
		catch(IOException e){
			/* Don't care. */
		}
	}
}
