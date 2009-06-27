package de.felixbruns.jotify.gateway.handlers;

import java.util.Map;

import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;

public class CheckHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("session")){
			String session = params.get("session");
			
			/* Check if session is valid. */
			if(GatewayApplication.sessions.containsKey(session)){
				return "<session>" + session + "</session>";
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
