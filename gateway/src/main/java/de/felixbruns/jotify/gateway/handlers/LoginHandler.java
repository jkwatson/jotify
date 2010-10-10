package de.felixbruns.jotify.gateway.handlers;

import java.util.Map;

import de.felixbruns.jotify.crypto.Hash;
import de.felixbruns.jotify.crypto.RandomBytes;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.gateway.GatewayConnection;
import de.felixbruns.jotify.gateway.GatewayApplication;
import de.felixbruns.jotify.gateway.GatewayHandler;
import de.felixbruns.jotify.util.Hex;

public class LoginHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Check if required parameters are present. */
		if(params.containsKey("username") && params.containsKey("password")){
			/* Create a new session. */
			GatewayConnection jotify = new GatewayConnection();
			
			/* Try to login. */
			try{
				jotify.login(params.get("username"), params.get("password"));
				
				/* Genrate a random session id. */
				byte[] random = new byte[256];
				
				RandomBytes.randomBytes(random);
				
				random         = Hash.sha1(random);
				String session = Hex.toHex(random);
				
				/* Add session to map. */
				GatewayApplication.sessions.put(session, jotify);
				GatewayApplication.executor.execute(jotify);
				
				return "<session>" + session + "</session>";
			}
			catch(ConnectionException e){
				return "<error>Can't connect!</error>";
			}
			catch(AuthenticationException e){
				return "<error>Authentication failed!</error>";
			}
		}
		else{
			return "<error>Invalid request parameters!</error>";
		}
	}
}
