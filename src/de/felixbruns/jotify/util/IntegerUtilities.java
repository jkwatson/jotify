package de.felixbruns.jotify.util;

/**
 * Class providing convenience methods for converting
 * between integers and byte arrays.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class IntegerUtilities {
	/**
	 * Convert an integer into a byte array.
	 * 
	 * @param i The integer.
	 * 
	 * @return A byte array with a length of 4.
	 */
	public static byte[] toBytes(int i){
		byte[] b = new byte[4];
		
		b[0] = (byte)(i >> 24);
		b[1] = (byte)(i >> 16);
		b[2] = (byte)(i >>  8);
		b[3] = (byte)(i);
		
		return b;
	}
	
	/**
	 * Convert a byte array into an unsigned integer.
	 * This method will use the first 4 bytes in the
	 * byte array.
	 * 
	 * @param b The byte array.
	 * 
	 * @return A long integer (unsigned integer).
	 */
	public static long bytesToUnsignedInteger(byte[] b){
		return bytesToUnsignedInteger(b, 0);
	}
	
	/**
	 * Convert a byte array into an unsigned integer.
	 * This method will use the next 4 bytes in the
	 * byte array, starting from the specified offset.
	 * 
	 * @param b   The byte array.
	 * @param off The offset in the byte array.
	 * 
	 * @return A long integer (unsigned integer).
	 */
	public static long bytesToUnsignedInteger(byte[] b, int off){
		/* Check length of byte array. */
		if(b.length < off + 4){
			throw new IllegalArgumentException("Not enough bytes in array!");
		}
		
		/* Convert and return value. */
		return  ((b[off    ] << 24) & 0xFFFFFFFF) |
				((b[off + 1] << 16) & 0x00FFFFFF) |
				((b[off + 2] <<  8) & 0x0000FFFF) |
				((b[off + 3]      ) & 0x000000FF);
	}
	
	/**
	 * Convert a byte array into a signed integer.
	 * This method will use the first 4 bytes in the
	 * byte array.
	 * 
	 * @param b The byte array.
	 * 
	 * @return An integer.
	 */
	public static int bytesToInteger(byte[] b){
		return bytesToInteger(b, 0);
	}
	
	/**
	 * Convert a byte array into a signed integer.
	 * This method will use the next 4 bytes in the
	 * byte array, starting from the specified offset.
	 * 
	 * @param b   The byte array.
	 * @param off The offset in the byte array.
	 * 
	 * @return An integer.
	 */
	public static int bytesToInteger(byte[] b, int off){
		/* Check length of byte array. */
		if(b.length < off + 4){
			throw new IllegalArgumentException("Not enough bytes in array!");
		}
		
		/* Convert and return value. */
		return 	((b[off    ] << 24) & 0xFFFFFFFF) |
				((b[off + 1] << 16) & 0x00FFFFFF) |
				((b[off + 2] <<  8) & 0x0000FFFF) |
				((b[off + 3]      ) & 0x000000FF);
	}
}
