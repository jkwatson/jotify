package de.felixbruns.jotify.gui.listeners;

import de.felixbruns.jotify.media.Track;

public interface PlayerListener {
	public void playerTrackChanged(Track track);
	public void playerStatusChanged(Status status);
	public void playerPositionChanged(int position);
	
	public enum Status {
		PLAY,
		PAUSE
	}
}
