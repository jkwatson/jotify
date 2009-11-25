package de.felixbruns.jotify.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides convenience methods to convert between base-62
 * encoded Spotify URIs and hexadecimal Spotify URIs.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class SpotifyURI {
	public enum Type {
		ARTIST, ALBUM, TRACK;
		
		public String toString(){
			return this.name().toLowerCase();
		}
	}
	
	public class InvalidSpotifyURIException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	private Type   type;
	private String id;
	
	public SpotifyURI(String uri) throws InvalidSpotifyURIException {
		/* Regex for matching Spotify URIs. */
		Pattern pattern = Pattern.compile("spotify:(artist|album|track):([0-9A-Za-z]{22})");
		Matcher matcher = pattern.matcher(uri);
		
		/* Check if URI matches. */
		if(matcher.matches()){
			String type = matcher.group(1);
			
			if(type.equals("artist")){
				this.type = Type.ARTIST;
			}
			else if(type.equals("album")){
				this.type = Type.ALBUM;
			}
			else if(type.equals("track")){
				this.type = Type.TRACK;
			}
			else{
				throw new InvalidSpotifyURIException();
			}
			
			this.id = matcher.group(2);
		}
		else{
			throw new InvalidSpotifyURIException();
		}
	}
	
	public Type getType(){
		return this.type;
	}
	
	public boolean isArtistURI(){
		return this.type.equals(Type.ARTIST);
	}
	
	public boolean isAlbumURI(){
		return this.type.equals(Type.ALBUM);
	}
	
	public boolean isTrackURI(){
		return this.type.equals(Type.TRACK);
	}
	
	public String getId(){
		return this.id;
	}
	
	public String toString() {
		return String.format("spotify:%s:%s", this.type, this.id);
	}
	
	/**
	 * Convert a base-62 encoded ID into a hexadecimal ID.
	 * 
	 * @param base62 The base-62 encoded ID.
	 * 
	 * @return A hexadecimal URI.
	 */
	public static String toHex(String base62){
		StringBuffer hex = new StringBuffer(BaseConvert.convert(base62, 62, 16));
		
		/* Prepend zeroes until hexadecimal string length is 32. */
		while(hex.length() < 32){
			hex.insert(0, '0');
		}
		
		return hex.toString();
	}
	
	/**
	 * Convert a hexadecimal ID into a base-62 encoded ID.
	 * 
	 * @param hex The hexadecimal ID.
	 * 
	 * @return A base-62 encoded ID.
	 */
	public static String toBase62(String hex){
		StringBuffer uri = new StringBuffer(BaseConvert.convert(hex, 16, 62));
		
		/* Prepend zeroes until base-62 string length is 22. */
		while(uri.length() < 22){
			uri.insert(0, '0');
		}
		
		return uri.toString();
	}
}
