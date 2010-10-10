package de.felixbruns.jotify.gateway.handlers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;

public class SearchHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("session") && params.containsKey("query")){
			String session = params.get("session");
			String query;
			
			try{
				query = URLDecoder.decode(params.get("query"), "UTF-8");
			}
			catch(UnsupportedEncodingException e){
				query = params.get("query");
			}
			
			/* Check if session is valid. */
			if(GatewayApplication.sessions.containsKey(session)){
				GatewayConnection jotify = GatewayApplication.sessions.get(session);
				
				/* Search. */
				try{
					return jotify.search(query);
				}
				catch(TimeoutException e){
					return "<error>" + e.getMessage() + "</error>";
				}
			}
			else{
				return "<error>Session not found!</error>";
			}
		}
		else{
			return "<error>Invalid request parameters!</error>";
		}
	}
}
