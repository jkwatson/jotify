package de.felixbruns.jotify.gateway.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.json.JSONException;
import org.json.XML;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.felixbruns.jotify.cache.Cache;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.util.URIUtilities;

public class ImageHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		/* Get request method and query. */
		String requestMethod = exchange.getRequestMethod();
		String requestQuery  = exchange.getRequestURI().getQuery();
		
		/* Get request parameters. */
		Map<String, String> params = URIUtilities.parseQuery(requestQuery);
		
		/* Get response body and headers. */
		OutputStream responseBody    = exchange.getResponseBody();
		Headers      responseHeaders = exchange.getResponseHeaders();
		String       responseString  = null;
		byte[]       responseBytes   = null;
		
		/* Set Access-Control headers.*/
		responseHeaders.set("Access-Control-Allow-Origin", "*");
		responseHeaders.set("Access-Control-Allow-Methods", "GET, OPTIONS");
		responseHeaders.set("Access-Control-Allow-Headers", "X-Requested-With");
		responseHeaders.set("Access-Control-Max-Age", "1728000");
		responseHeaders.set("Cache-Control", "max-age=604800");
		
		/* OPTIONS. */
		if(requestMethod.equalsIgnoreCase("OPTIONS")){
			responseString = "";
		}
		/* GET. */
		else if(requestMethod.equalsIgnoreCase("GET")){
			/* Check if required parameters are present. */
			if(params.containsKey("session") &&
			   params.containsKey("id")      &&
			   params.get("session").equals("cache")){
				Cache  cache = new FileCache();
				String id    = params.get("id");
				
				if(cache.contains("image", id)){
					responseBytes = cache.load("image", id);
				}
				else{
					responseString = "<error>Image not cached, please supply a session!</error>";
				}
			}
			else if(params.containsKey("session") &&
					params.containsKey("id")){
				String session = params.get("session");
				String id      = params.get("id");
				
				/* Check if session is valid. */
				if(GatewayApplication.sessions.containsKey(session)){
					GatewayConnection jotify = GatewayApplication.sessions.get(session);
					
					/* Load image. */
					try{
						responseBytes = jotify.image(id);
					}
					catch(RuntimeException e){
						responseString = "<error>" + e.getCause().getMessage() + "</error>";
					}
				}
				else{
					responseString = "<error>Session not found!</error>";
				}
			}
			else{
				responseString = "<error>Invalid request parameters!</error>";
			}
		}
		else{
			responseString = "<error>Method not supported!</error>";
		}
		
		/* Set Content-Type header and encode response depending on output format. */
		if(responseBytes != null){
			responseHeaders.set("Content-Type", "image/jpeg");
		}
		else if(params.get("format") != null && params.get("format").equals("json")){
			responseHeaders.set("Content-Type", "application/json");
			
			try{
				responseString = XML.toJSONObject(responseString).toString(4);
			}
			catch(JSONException e){
				responseString = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
			}
			
			/* Get response bytes. */
			responseBytes = responseString.getBytes(Charset.forName("UTF-8"));
		}
		else{
			responseHeaders.set("Content-Type", "text/xml");
			
			/* Get response bytes. */
			responseBytes = responseString.getBytes(Charset.forName("UTF-8"));
		}
		
		/* Send response code, length and headers. */
		exchange.sendResponseHeaders(200, responseBytes.length);
		
		/* Write response string to output stream. */
		responseBody.write(responseBytes);
		responseBody.close();
	}
}
