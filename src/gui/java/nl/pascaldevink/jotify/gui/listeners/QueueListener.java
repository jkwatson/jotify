package nl.pascaldevink.jotify.gui.listeners;

import nl.pascaldevink.jotify.gui.JotifyPlaybackQueue;

public interface QueueListener {
	public void queueSelected(JotifyPlaybackQueue queue);
	public void queueUpdated(JotifyPlaybackQueue queue);
}
