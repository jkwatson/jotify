package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StreamHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String response = null;
		OutputStream body = exchange.getResponseBody();
		Headers headers = exchange.getResponseHeaders();
		
		headers.set("Content-Type", "text/plain");
				
		headers.set("Access-Control-Allow-Origin", "*");
		headers.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
		headers.set("Access-Control-Allow-Headers", "X-Requested-With");
		headers.set("Access-Control-Max-Age", "1728000");
		
		if(method.equalsIgnoreCase("GET")){
			Map<String, String> params = URIUtilities.parseQuery(exchange.getRequestURI().getQuery());
			
			if(params.containsKey("session") && params.containsKey("id") && params.containsKey("file")){
				String session = params.get("session");
				String id      = params.get("id");
				String file    = params.get("file");
				
				if(JotifyGateway.sessions.containsKey(session)){
					GatewaySession jotify = JotifyGateway.sessions.get(session);
					
					headers.set("Content-Type", "audio/ogg");
					
					jotify.stream(id, file, body);
					
					exchange.sendResponseHeaders(200, 0);
				}
				else{
					response = "error Session not found.";
				}
			}
			else{
				response = "error Invalid request parameters.";
			}
		}
		else if(method.equalsIgnoreCase("OPTIONS")){
			response = "";
		}
		else{
			response = "error Method not supported.";
		}
		
		if(response != null){
			exchange.sendResponseHeaders(200, response.length());
			
			body.write(response.getBytes());
			body.close();
		}
	}
}
