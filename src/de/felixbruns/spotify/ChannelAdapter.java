package de.felixbruns.spotify;

public class ChannelAdapter implements ChannelListener {
	public void channelData(Channel channel, byte[] data){}
	public void channelEnd(Channel channel){}
	public void channelError(Channel channel){}
	public void channelHeader(Channel channel, byte[] header){}
}
