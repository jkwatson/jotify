package de.felixbruns.jotify.player;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.LineUnavailableException;

import de.felixbruns.jotify.media.Track;

public interface Player {
	public void play(Track track, int bitrate, PlaybackListener listener) throws TimeoutException, IOException, LineUnavailableException;
	public void play();
	public void pause();
	public void stop();
	public int length();
	public int position();
	public void seek(int ms) throws IOException;
	public float volume();
	public void volume(float volume);
}
