package de.felixbruns.jotify.gui.util;

public class TimeFormatter {
	public static String formatSeconds(int seconds){
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}
	
	public static String formatRemainingSeconds(int seconds){
		return String.format("-%02d:%02d", seconds / 60, seconds % 60);
	}
}
