package de.felixbruns.jotify.protocol.channel;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ChannelCallback implements ChannelListener {
	private Semaphore        done;
	private List<ByteBuffer> buffers;
	private int              bytes;
	
	public ChannelCallback(){
		this.done    = new Semaphore(1);
		this.buffers = new LinkedList<ByteBuffer>();
		this.bytes   = 0;
		
		this.done.acquireUninterruptibly();
	}
	
	public void channelData(Channel channel, byte[] data){
		ByteBuffer buffer = ByteBuffer.wrap(data);
		
		this.bytes += data.length;
		
		this.buffers.add(buffer);
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
		
		/* Data buffer. */
		ByteBuffer data = ByteBuffer.allocate(this.bytes);
		
		for(ByteBuffer b : this.buffers){
			data.put(b);
		}
		
		/* Return data bytes. */
		return data.array();
	}
}
