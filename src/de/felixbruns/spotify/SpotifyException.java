package de.felixbruns.spotify;

@SuppressWarnings("serial")
public class SpotifyException extends Exception {
	public SpotifyException(String message){
		super(message);
	}
	
	public SpotifyException(String format, Object... arguments){
		super(String.format(format, arguments));
	}
}
