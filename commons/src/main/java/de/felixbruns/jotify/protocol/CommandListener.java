package de.felixbruns.jotify.protocol;

public interface CommandListener {
	public void commandReceived(int command, byte[] payload);
}
