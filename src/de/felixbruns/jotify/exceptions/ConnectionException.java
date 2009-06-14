package de.felixbruns.jotify.exceptions;

/**
 * An exception that is thrown if something is wrong with the connection.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 *
 * @category Exceptions
 */
public class ConnectionException extends Exception {
	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct a new {@link ConnectionException} using a message string.
	 * 
	 * @param message The message.
	 */
	public ConnectionException(String message){
		super(message);
	}
	
	/**
	 * Construct a new {@link ConnectionException} using a cause.
	 * 
	 * @param cause The cause.
	 */
	public ConnectionException(Throwable cause){
		super(cause);
	}
	
	/**
	 * Construct a new {@link ConnectionException} using a message string and a cause.
	 * 
	 * @param message The message.
	 * @param cause   The cause.
	 */
	public ConnectionException(String message, Throwable cause){
		super(message, cause);
	}
}
