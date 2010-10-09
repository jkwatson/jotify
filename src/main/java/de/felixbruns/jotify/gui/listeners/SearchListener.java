package de.felixbruns.jotify.gui.listeners;

import de.felixbruns.jotify.media.Result;

public interface SearchListener {
	public void searchResultReceived(Result result);
	public void searchResultSelected(Result result);
}
