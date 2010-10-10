package de.felixbruns.jotify.exceptions;

/**
 * An exception that is thrown if something is wrong with the protocol.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 *
 * @category Exceptions
 */
public class ProtocolException extends Exception {
	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct a new {@link ProtocolException} using a message string.
	 * 
	 * @param message The message.
	 */
	public ProtocolException(String message){
		super(message);
	}
	
	/**
	 * Construct a new {@link ProtocolException} using a cause.
	 * 
	 * @param cause The cause.
	 */
	public ProtocolException(Throwable cause){
		super(cause);
	}
	
	/**
	 * Construct a new {@link ProtocolException} using a message string and a cause.
	 * 
	 * @param message The message.
	 * @param cause   The cause.
	 */
	public ProtocolException(String message, Throwable cause){
		super(message, cause);
	}
}
