package de.felixbruns.jotify.util;

/**
 * Class providing convenience methods for converting between short integers and byte arrays.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class ShortUtilities {
	/**
	 * Convert a short integer into a byte array.
	 * 
	 * @param i The short integer.
	 * 
	 * @return A byte array with a length of 2.
	 */
	public static byte[] toBytes(short i){
		byte[] b = new byte[2];
		
		b[0] = (byte)(i >> 8);
		b[1] = (byte)(i);
		
		return b;
	}
	
	/**
	 * Convert a byte array into an unsigned short integer.
	 * This method will use the first 2 bytes in the byte array.
	 * 
	 * @param b The byte array.
	 * 
	 * @return An integer (unsigned short integer).
	 */
	public static int bytesToUnsignedShort(byte[] b){
		return bytesToUnsignedShort(b, 0);
	}
	
	/**
	 * Convert a byte array into a unsigned short integer.
	 * This method will use the next 2 bytes in the byte array,
	 * starting from the specified offset.
	 * 
	 * @param b   The byte array.
	 * @param off The offset in the byte array.
	 * 
	 * @return An integer (unsigned short integer).
	 */
	public static int bytesToUnsignedShort(byte[] b, int off){
		/* Check length of byte array. */
		if(b.length < off + 2){
			throw new IllegalArgumentException("Not enough bytes in array!");
		}
		
		/* Convert and return value. */
		return (((b[off    ] << 8) & 0xFFFF) |
				((b[off + 1]     ) & 0x00FF));
	}
	
	/**
	 * Convert a byte array into an signed short integer.
	 * This method will use the first 2 bytes in the byte array.
	 * 
	 * @param b The byte array.
	 * 
	 * @return A short integer.
	 */
	public static short bytesToShort(byte[] b){
		return bytesToShort(b, 0);
	}
	
	/**
	 * Convert a byte array into a signed short integer.
	 * This method will use the next 2 bytes in the byte array,
	 * starting from the specified offset.
	 * 
	 * @param b   The byte array.
	 * @param off The offset in the byte array.
	 * 
	 * @return A short integer.
	 */
	public static short bytesToShort(byte[] b, int off){
		/* Check length of byte array. */
		if(b.length < off + 2){
			throw new IllegalArgumentException("Not enough bytes in array!");
		}
		
		/* Convert and return value. */
		return (short)(((b[off    ] << 8) & 0xFFFF) |
						(b[off + 1]     ) & 0x00FF);
	}
}
