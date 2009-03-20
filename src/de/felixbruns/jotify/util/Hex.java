package de.felixbruns.jotify.util;

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
	
	public static String URIToId(String uri) {
		StringBuffer x = new StringBuffer(baseConvert(uri, 62, 16));

		while (x.length() < 32)
			x.insert(0, '0');

		return x.toString();
	}

	public static String IdToURI(String id) {
		StringBuffer x = new StringBuffer(baseConvert(id, 16, 62));

		while (x.length() < 22)
			x.insert(0, '0');

		return x.toString();
	}

	private static String baseConvert(String source, int frombase, int tobase) {
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String tostring = chars.substring(0, tobase);

		int length = source.length();
		String result = "";
		int[] number = new int[length];

		for (int i = 0; i < length; i++) {
			number[i] = chars.indexOf(source.charAt(i));
		}

		int divide;
		int newlen;

		do {
			divide = 0;
			newlen = 0;

			for (int i = 0; i < length; i++) {
				divide = divide * frombase + number[i];

				if (divide >= tobase) {
					number[newlen++] = (int) (divide / tobase);
					divide = divide % tobase;
				} else if (newlen > 0) {
					number[newlen++] = 0;
				}
			}

			length = newlen;
			result = tostring.charAt(divide) + result;
		} while (newlen != 0);

		return result;
	}
}
