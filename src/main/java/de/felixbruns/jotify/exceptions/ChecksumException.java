package de.felixbruns.jotify.exceptions;

/**
 * An exception that is thrown if a checksum doesn't match.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 *
 * @category Exceptions
 */
public class ChecksumException extends Exception {
	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct a new {@link ChecksumException} using a message string.
	 * 
	 * @param message The message.
	 */
	public ChecksumException(String message){
		super(message);
	}
	
	/**
	 * Construct a new {@link ChecksumException} using a cause.
	 * 
	 * @param cause The cause.
	 */
	public ChecksumException(Throwable cause){
		super(cause);
	}
	
	/**
	 * Construct a new {@link ChecksumException} using a message string and a cause.
	 * 
	 * @param message The message.
	 * @param cause   The cause.
	 */
	public ChecksumException(String message, Throwable cause){
		super(message, cause);
	}
}
