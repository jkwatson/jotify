package de.felixbruns.jotify.gui.listeners;

import de.felixbruns.jotify.gui.JotifyPlaybackQueue;

public interface QueueListener {
	public void queueSelected(JotifyPlaybackQueue queue);
	public void queueUpdated(JotifyPlaybackQueue queue);
}
