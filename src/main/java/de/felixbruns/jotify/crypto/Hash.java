package de.felixbruns.jotify.crypto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class providing convenience methods for hashing data.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class Hash {
	private static MessageDigest digestSha1;
	private static MessageDigest digestMd5;
	private static Mac           hmacSha1;
	
	/**
	 * Statically instantiate needed objects.
	 */
	static{
		try{
			digestSha1 = MessageDigest.getInstance("SHA-1");
			digestMd5  = MessageDigest.getInstance("MD5");
			hmacSha1   = Mac.getInstance("HmacSHA1");
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Compute the SHA-1 hash of a buffer.
	 * 
	 * @param buffer The buffer of bytes.
	 * 
	 * @return The 20-byte SHA-1 hash of that buffer.
	 */
	public static byte[] sha1(byte[] buffer){
		if(digestSha1 == null){
			throw new RuntimeException("MessageDigest not instantiated!");
		}
		
		return digestSha1.digest(buffer);
	}
	
	/**
	 * Compute the MD5 hash of a buffer.
	 * 
	 * @param buffer The buffer of bytes.
	 * 
	 * @return The 16-byte MD5 hash of that buffer.
	 */
	public static byte[] md5(byte[] buffer){
		if(digestMd5 == null){
			throw new RuntimeException("MessageDigest not instantiated!");
		}
		
		return digestMd5.digest(buffer);
	}
	
	/**
	 * Compute the SHA-1 HMAC of a buffer.
	 * 
	 * @param buffer The buffer of bytes.
	 * @param key    The key to use for keying.
	 * 
	 * @return The 20-byte SHA-1 HMAC of that buffer.
	 */
	public static byte[] hmacSha1(byte[] buffer, byte[] key){
		byte[] output = new byte[20];
		
		hmacSha1(buffer, key, output, 0);
		
		return output;
	}
	
	/**
	 * Compute the SHA-1 HMAC of a buffer.
	 * 
	 * @param buffer The buffer of bytes.
	 * @param key    The key to use for keying.
	 * @param output The destination buffer.
	 * @param offset The offset in the destination buffer.
	 */
	public static void hmacSha1(byte[] buffer, byte[] key, byte[] output, int offset){
		if(hmacSha1 == null){
			throw new RuntimeException("Mac not instantiated!");
		}
		
		/* Create secret key from bytes. */
		SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA1");
		
		/* Initialize Mac with secret key. */
		try{
			hmacSha1.init(secretKey);
		}
		catch(InvalidKeyException e){
			throw new RuntimeException(e);
		}
		
		/* Update Mac with buffer. */
		hmacSha1.update(buffer);
		
		/* Write output into buffer at specified offset. */
		try{
			hmacSha1.doFinal(output, offset);
		}
		catch(ShortBufferException e){
			throw new RuntimeException(e);
		}
		catch(IllegalStateException e){
			throw new RuntimeException(e);
		}
	}
}
