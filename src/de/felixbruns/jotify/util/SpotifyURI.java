package de.felixbruns.jotify.util;

public class SpotifyURI {
	public static String toHex(String uri){
		StringBuffer hex = new StringBuffer(baseConvert(uri, 62, 16));

		while(hex.length() < 32){
			hex.insert(0, '0');
		}

		return hex.toString();
	}

	public static String toURI(String hex){
		StringBuffer uri = new StringBuffer(baseConvert(hex, 16, 62));
		
		while(uri.length() < 22){
			uri.insert(0, '0');
		}
		
		return uri.toString();
	}

	private static String baseConvert(String source, int from, int to) {
		String chars  = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(0, to);
		String result = "";
		int    length = source.length();
		int[]  number = new int[length];
		
		for(int i = 0; i < length; i++){
			number[i] = chars.indexOf(source.charAt(i));
		}

		int divide;
		int newlen;

		do{
			divide = 0;
			newlen = 0;

			for (int i = 0; i < length; i++){
				divide = divide * from + number[i];

				if(divide >= to){
					number[newlen++] = (int)(divide / to);
					divide = divide % to;
				}
				else if(newlen > 0){
					number[newlen++] = 0;
				}
			}
			
			length = newlen;
			result = chars.charAt(divide) + result;
		}
		while(newlen != 0);
		
		return result;
	}
}
