package de.felixbruns.jotify.player;

import de.felixbruns.jotify.media.Track;

public interface PlaybackListener {
	public void playbackStarted(Track track);
	public void playbackStopped(Track track);
	public void playbackResumed(Track track);
	public void playbackPosition(Track track, int ms);
	public void playbackFinished(Track track);
}
