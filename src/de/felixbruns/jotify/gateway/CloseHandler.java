package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.felixbruns.jotify.exceptions.ConnectionException;

public class CloseHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String response;
		OutputStream body = exchange.getResponseBody();
		Headers headers = exchange.getResponseHeaders();
		
		headers.set("Content-Type", "text/plain");
		
		headers.set("Access-Control-Allow-Origin", "*");
		headers.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
		headers.set("Access-Control-Allow-Headers", "X-Requested-With");
		headers.set("Access-Control-Max-Age", "1728000");
		
		if(method.equalsIgnoreCase("GET")){
			Map<String, String> params = URIUtilities.parseQuery(exchange.getRequestURI().getQuery());
			
			if(params.containsKey("session")){
				String session = params.get("session");
				
				if(JotifyGateway.sessions.containsKey(session)){
					GatewaySession jotify = JotifyGateway.sessions.remove(session);
					
					try{
						jotify.close();
					}
					catch(ConnectionException e){
						/* Ignore. */
					}
					
					response = "success";
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
		
		exchange.sendResponseHeaders(200, response.length());
		
		body.write(response.getBytes());
		body.close();
	}
}
