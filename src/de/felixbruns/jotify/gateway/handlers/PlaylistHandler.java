package de.felixbruns.jotify.gateway.handlers;

import java.util.Map;

import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;

public class PlaylistHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("session") && params.containsKey("id")){
			String session = params.get("session");
			String id      = params.get("id");
			
			/* Check if session is valid. */
			if(GatewayApplication.sessions.containsKey(session)){
				GatewayConnection jotify = GatewayApplication.sessions.get(session);
				
				/* Get playlist. */
				return jotify.playlist(id);
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
