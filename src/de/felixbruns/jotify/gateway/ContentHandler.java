package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContentHandler implements HttpHandler {
	private String resource;
	private String contentType;
	
	public ContentHandler(String resource, String contentType){
		this.resource    = resource;
		this.contentType = contentType;
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		Headers      headers  = exchange.getResponseHeaders();
		OutputStream body     = exchange.getResponseBody();
		byte[]       buffer   = new byte[1024];
		int          read;
		
		/* Set Content-Type header. */
		headers.set("Content-Type", this.contentType);
		
		/* Get resource as stream. */
		InputStream stream = JotifyGateway.class.getResourceAsStream(this.resource);
		
		/* Send response header with Content-Length. */
		exchange.sendResponseHeaders(200, stream.available());
		
		/* Write response to output stream. */
		while((read = stream.read(buffer)) != -1){
			body.write(buffer, 0, read);
		}
		
		/* Close output stream. */
		body.close();
	}
}
