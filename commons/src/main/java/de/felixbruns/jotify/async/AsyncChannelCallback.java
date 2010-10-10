package de.felixbruns.jotify.async;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;
import de.felixbruns.jotify.util.GZIP;

public abstract class AsyncChannelCallback implements Runnable, ChannelListener {
	private List<ByteBuffer> buffers;
	private int              bytes;
	
	private static Executor executor;
	
	static {
		executor = Executors.newCachedThreadPool();
	}
	
	public AsyncChannelCallback(){
		this.buffers = new LinkedList<ByteBuffer>();
		this.bytes   = 0;
	}
	
	public void channelHeader(Channel channel, byte[] header){
		/* Ignore */
	}
	
	public void channelData(Channel channel, byte[] data){
		ByteBuffer buffer = ByteBuffer.wrap(data);
		
		this.bytes += data.length;
		
		this.buffers.add(buffer);
	}
	
	public void channelEnd(Channel channel){
		Channel.unregister(channel.getId());
		
		/* Ensure callback is not executed in I/O thread. */
		executor.execute(this);
	}
	
	public void channelError(Channel channel){
		Channel.unregister(channel.getId());
	}
	
	public void run(){
		/* Invoke callback. */
		this.receivedData(this.getData());
	}
	
	/**
	 * Get received bytes from this channel and automatically inflate
	 * the data when GZIP magic is detected.
	 * 
	 * @return An array of bytes.
	 */
	private byte[] getData(){
		/* Data buffer. */
		ByteBuffer data = ByteBuffer.allocate(this.bytes);
		
		for(ByteBuffer b : this.buffers){
			data.put(b);
		}
		
		/* Get data bytes. */
		byte[] bytes = data.array();
		
		/* Detect GZIP magic and return inflated data. */
		if(bytes.length >= 2 && bytes[0] == (byte)0x1f && bytes[1] == (byte)0x8b){
			return GZIP.inflate(bytes);
		}
		
		/* Return data. */
		return bytes;
	}
	
	public abstract void receivedData(byte[] data);
}
