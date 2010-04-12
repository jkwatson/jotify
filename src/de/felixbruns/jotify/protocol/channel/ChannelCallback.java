package de.felixbruns.jotify.protocol.channel;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.felixbruns.jotify.util.GZIP;

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
		
		this.done.release();
	}
	
	public void channelError(Channel channel){
		this.done.release();
	}
	
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
	
	public byte[] get(){
		/* Wait for data to become available. */
		this.done.acquireUninterruptibly();
		
		/* Return data array. */
		return this.getData();
	}
	
	public byte[] get(long timeout, TimeUnit unit) throws TimeoutException {
		/* Wait for data to become available. */
		try{
			if(!this.done.tryAcquire(timeout, unit)){
				throw new TimeoutException("Timeout while waiting for data.");
			}
			
			return this.getData();
		}
		catch(InterruptedException e){
			return null;
		}
	}
	
	public boolean isDone(){
		return this.done.availablePermits() > 0;
	}
}
