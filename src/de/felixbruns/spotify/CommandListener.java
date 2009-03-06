package de.felixbruns.spotify;

public interface CommandListener {
	public void commandReceived(int command, byte[] payload);
}
