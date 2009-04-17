package de.felixbruns.jotify.util;

public class Hex {
	/* Safe with leading zeroes (unlike BigInteger) and with negative byte values (unlike Byte.parseByte). */
	public static byte[] toBytes(String hex){
		if(hex.length() % 2 != 0){
			throw new IllegalArgumentException("Input string must contain an even number of characters");
		}
		
		byte[] bytes = new byte[hex.length() / 2];
		
		for(int i = 0; i < hex.length(); i += 2){
			bytes[i / 2] = (byte)(
				(Character.digit(hex.charAt(i    ), 16) << 4) +
				 Character.digit(hex.charAt(i + 1), 16)
			);
		}
		
		return bytes;
	}
	
	public static String toHex(byte[] bytes){
		String hex = "";
		
		for(int i = 0; i < bytes.length; i++){
			hex += String.format("%02x", bytes[i]);
		}
		
		return hex;
	}
}
