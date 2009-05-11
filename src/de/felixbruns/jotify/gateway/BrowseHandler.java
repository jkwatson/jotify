package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.felixbruns.jotify.gateway.GatewaySession.BrowseType;

public class BrowseHandler implements HttpHandler {
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
			
			if(params.containsKey("session") && params.containsKey("type") && params.containsKey("id")){
				String session = params.get("session");
				int    type    = Integer.parseInt(params.get("type"));
				String id      = params.get("id");
				
				if(JotifyGateway.sessions.containsKey(session)){
					GatewaySession jotify = JotifyGateway.sessions.get(session);
					
					headers.set("Content-Type", "text/xml");
					
					response = jotify.browse(BrowseType.valueOf(type), id);
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
