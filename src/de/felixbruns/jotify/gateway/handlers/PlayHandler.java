package de.felixbruns.jotify.gateway.handlers;

import java.util.Map;

import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;
import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Track;

public class PlayHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("session") && params.containsKey("id") && params.containsKey("file")){
			String session = params.get("session");
			String id      = params.get("id");
			String file    = params.get("file");
			
			/* Check if session is valid. */
			if(GatewayApplication.sessions.containsKey(session)){
				GatewayConnection jotify = GatewayApplication.sessions.get(session);
				
				/* Play. */
				Track track = new Track(id);
					
				track.addFile(new File(file, null));
				
				jotify.stop();
				jotify.play(track, null);
				
				return "<ok />";
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
