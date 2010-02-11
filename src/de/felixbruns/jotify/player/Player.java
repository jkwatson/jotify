package de.felixbruns.jotify.player;

import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.media.Track;

public interface Player {
	public void play(Track track, PlaybackListener listener) throws TimeoutException;
	public void play();
	public void pause();
	public void stop();
	public int length();
	public int position();
	public float volume();
	public void volume(float volume);
}
