package de.felixbruns.spotify.util;

import java.math.BigInteger;
import java.util.Arrays;

public class Hex {
	public static byte[] toBytes(String hex){
		byte[] bytes = new BigInteger(hex, 16).toByteArray();
		
		if(bytes.length != hex.length() / 2 && bytes[0] == 0x00){
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}
		
		return bytes;
	}
	
	public static String toHex(byte[] bytes){
		return new BigInteger(bytes).toString(16);
	}
}
