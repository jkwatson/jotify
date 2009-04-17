package de.felixbruns.jotify.exceptions;

@SuppressWarnings("serial")
public class AuthenticationException extends Exception {
	public AuthenticationException(String message){
		super(message);
	}
}
