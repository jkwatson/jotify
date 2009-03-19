package de.felixbruns.jotify.protocol.channel;

public interface ChannelListener {
	public void channelHeader(Channel channel, byte[] header);
	public void channelData(Channel channel, byte[] data);
	public void channelError(Channel channel);
	public void channelEnd(Channel channel);
}
