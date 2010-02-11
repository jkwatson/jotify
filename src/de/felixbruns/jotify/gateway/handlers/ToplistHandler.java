package de.felixbruns.jotify.gateway.handlers;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;

public class ToplistHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("session") && params.containsKey("type")){
			String session  = params.get("session");
			String type     = params.get("type");
			String region   = params.get("region");
			String username = params.get("username");
			
			/* Check if session is valid. */
			if(GatewayApplication.sessions.containsKey(session)){
				GatewayConnection jotify = GatewayApplication.sessions.get(session);
				
				/* Get toplist. */
				try{
					return jotify.toplist(type, region, username);
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
