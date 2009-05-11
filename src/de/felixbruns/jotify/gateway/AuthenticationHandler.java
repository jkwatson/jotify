package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.felixbruns.jotify.crypto.Hash;
import de.felixbruns.jotify.crypto.RandomBytes;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.util.Hex;

public class AuthenticationHandler implements HttpHandler {
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
			
			if(params.containsKey("username") && params.containsKey("password")){
				GatewaySession jotify = new GatewaySession();
				
				try{
					jotify.login(params.get("username"), params.get("password"));
					
					byte[] random = new byte[256];
					RandomBytes.randomBytes(random);
					random = Hash.sha1(random);
					String session = Hex.toHex(random);
					
					JotifyGateway.sessions.put(session, jotify);
					JotifyGateway.executor.execute(jotify);
					
					response = "session " + session;
				}
				catch(ConnectionException e){
					response = "error Can't connect.";
				}
				catch(AuthenticationException e){
					response = "error Authentication failed.";
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
