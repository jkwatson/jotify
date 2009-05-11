package de.felixbruns.jotify.util;

public class SpotifyURI {
	public static String toHex(String uri){
		StringBuffer hex = new StringBuffer(BaseConvert.convert(uri, 62, 16));
		
		while(hex.length() < 32){
			hex.insert(0, '0');
		}
		
		return hex.toString();
	}

	public static String toURI(String hex){
		StringBuffer uri = new StringBuffer(BaseConvert.convert(hex, 16, 62));
		
		while(uri.length() < 22){
			uri.insert(0, '0');
		}
		
		return uri.toString();
	}
}
