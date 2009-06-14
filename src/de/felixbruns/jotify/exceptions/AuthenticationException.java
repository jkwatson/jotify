package de.felixbruns.jotify.exceptions;

/**
 * An exception that is thrown if authentication failed.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 *
 * @category Exceptions
 */
public class AuthenticationException extends Exception {
	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct a new {@link AuthenticationException} using a message string.
	 * 
	 * @param message The message.
	 */
	public AuthenticationException(String message){
		super(message);
	}
	
	/**
	 * Construct a new {@link AuthenticationException} using a cause.
	 * 
	 * @param cause The cause.
	 */
	public AuthenticationException(Throwable cause){
		super(cause);
	}
	
	/**
	 * Construct a new {@link AuthenticationException} using a message string and a cause.
	 * 
	 * @param message The message.
	 * @param cause   The cause.
	 */
	public AuthenticationException(String message, Throwable cause){
		super(message, cause);
	}
}
