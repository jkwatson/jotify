package de.felixbruns.jotify.util;

/**
 * Provides convenience methods to convert between base-62
 * encoded Spotify URIs and hexadecimal Spotify URIs.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class SpotifyURI {
	/**
	 * Convert a base-62 encoded URI into a hexadecimal URI.
	 * 
	 * @param uri The base-62 encoded URI.
	 * 
	 * @return A hexadecimal URI.
	 */
	public static String toHex(String uri){
		StringBuffer hex = new StringBuffer(BaseConvert.convert(uri, 62, 16));
		
		/* Prepend zeroes until hexadecimal string length is 32. */
		while(hex.length() < 32){
			hex.insert(0, '0');
		}
		
		return hex.toString();
	}
	
	/**
	 * Convert a hexadecimal URI into a base-62 encoded URI.
	 * 
	 * @param hex The hexadecimal URI.
	 * 
	 * @return A base-62 encoded URI.
	 */
	public static String toURI(String hex){
		StringBuffer uri = new StringBuffer(BaseConvert.convert(hex, 16, 62));
		
		/* Prepend zeroes until base-62 string length is 22. */
		while(uri.length() < 22){
			uri.insert(0, '0');
		}
		
		return uri.toString();
	}
}
