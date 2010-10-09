package de.felixbruns.jotify.gateway.handlers;

import java.util.List;
import java.util.Map;

import de.felixbruns.jotify.cache.Cache;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.gateway.GatewayHandler;

public class StartHandler extends GatewayHandler {
	public String handle(Map<String, String> params){
		/* Get local file cache and list image hashes. */
		Cache        cache  = new FileCache();
		List<String> hashes = cache.list("image");
		String       response;
		
		/* Build response. */
		response = "<images>";
		
		for(String hash : hashes){
			response += "<image>" + hash + "</image>";
		}
		
		response += "</images>";
		
		return response;
	}
}
