package de.felixbruns.jotify.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
		ARTIST, ALBUM, TRACK, PLAYLIST, SEARCH;
		
		public String toString(){
			return this.name().toLowerCase();
		}
	}
	
	public class InvalidSpotifyURIException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	private static final Pattern mediaPattern    = Pattern.compile("spotify:(artist|album|track):([0-9A-Za-z]{22})");
	private static final Pattern playlistPattern = Pattern.compile("spotify:user:([^:]+):playlist:([0-9A-Za-z]{22})");
	private static final Pattern searchPattern   = Pattern.compile("spotify:search:([^\\s]+)");
	
	private Type   type;
	private String user;
	private String id;
	private String query;
	
	public SpotifyURI(String uri) throws InvalidSpotifyURIException {
		/* Regex for matching Spotify URIs. */
		Matcher mediaMatcher    = mediaPattern.matcher(uri);
		Matcher playlistMatcher = playlistPattern.matcher(uri);
		Matcher searchMatcher   = searchPattern.matcher(uri);
		
		/* Check if URI matches. */
		if(mediaMatcher.matches()){
			String type = mediaMatcher.group(1);
			
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
			
			this.id = mediaMatcher.group(2);
		}
		else if(playlistMatcher.matches()){
			this.type = Type.PLAYLIST;
			this.user = playlistMatcher.group(1);
			this.id   = playlistMatcher.group(2);
		}
		else if(searchMatcher.matches()){
			this.type = Type.SEARCH;
			
			try{
				this.query = URLDecoder.decode(searchMatcher.group(1), "UTF-8");
			}
			catch(UnsupportedEncodingException e){
				throw new InvalidSpotifyURIException();
			}
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
	
	public boolean isPlaylistURI(){
		return this.type.equals(Type.PLAYLIST);
	}
	
	public boolean isSearchURI(){
		return this.type.equals(Type.SEARCH);
	}
	
	public String getUser(){
		if(!this.isPlaylistURI()){
			throw new IllegalStateException("Not a playlist URI!");
		}
		
		return this.user;
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getHexId(){
		return toHex(this.id);
	}
	
	public String getQuery(){
		if(!this.isSearchURI()){
			throw new IllegalStateException("Not a search URI!");
		}
		
		return this.query;
	}
	
	
	public String toString(){
		if(this.isPlaylistURI()){
			return String.format("spotify:user:%s:playlist:%s", this.user, this.id);
		}
		else if(this.isSearchURI()){
			return String.format("spotify:search:%s", this.query);
		}
		else{
			return String.format("spotify:%s:%s", this.type, this.id);
		}
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
