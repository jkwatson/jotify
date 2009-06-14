package de.felixbruns.jotify.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Class providing convenience methods for generating random byte sequences.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class RandomBytes {
	/**
	 * {@link SecureRandom} needed for generating random bytes.
	 */
	private static SecureRandom secureRandom;
	
	/**
	 * Statically instantiate needed objects.
	 */
	static{
		try{
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Fill the supplied buffer with random bytes.
	 * 
	 * @param buffer The buffer to be filled with random bytes.
	 */
	public static void randomBytes(byte[] buffer){
		/* Check if secure random is instantiated. */
		if(secureRandom == null){
			throw new RuntimeException("SecureRandom not instantiated!");
		}
		
		/* Fill buffer with random bytes. */
		secureRandom.nextBytes(buffer);
	}
	
	/**
	 * Create a sequence of random bytes with the supplied length.
	 * 
	 * @param length The length of the random byte sequence.
	 * 
	 * @return A buffer containing the random bytes.
	 */
	public static byte[] randomBytes(int length){
		/* Create a buffer of the specified length. */
		byte[] buffer = new byte[length];
		
		/* Check if secure random is instantiated. */
		if(secureRandom == null){
			throw new RuntimeException("SecureRandom not instantiated!");
		}
		
		/* Fill buffer with random bytes. */
		secureRandom.nextBytes(buffer);
		
		return buffer;
	}
}
