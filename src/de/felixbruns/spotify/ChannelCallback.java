package de.felixbruns.spotify;

import java.util.concurrent.Semaphore;

import de.felixbruns.spotify.util.Buffer;

public class ChannelCallback implements ChannelListener {
	private Semaphore done;
	private Buffer    buffer;
	
	public ChannelCallback(){
		this.done   = new Semaphore(1);
		this.buffer = new Buffer();
		
		this.done.acquireUninterruptibly();
	}
	
	public void channelData(Channel channel, byte[] data){
		this.buffer.appendBytes(data);
	}
	
	public void channelEnd(Channel channel){
		Channel.unregister(channel.getId());
		
		this.done.release();
	}
	
	public void channelError(Channel channel){
		this.done.release();
	}
	
	public void channelHeader(Channel channel, byte[] header){
		/* Ignore */
	}
	
	public byte[] getData(){
		/* Wait for data to become available. */
		this.done.acquireUninterruptibly();
		
		/* Return data bytes. */
		return this.buffer.getBytes();
	}
}
