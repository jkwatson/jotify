package de.felixbruns.jotify.protocol.channel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChannelHeaderCallback implements ChannelListener {
	private Semaphore    done;
	private List<String> urls;
	
	public ChannelHeaderCallback(){
		this.done = new Semaphore(1);
		this.urls = new ArrayList<String>();
		
		this.done.acquireUninterruptibly();
	}
	
	public void channelHeader(Channel channel, byte[] header){
		/* HTTP stream. */
		if(header[0] == 0x07){
			/* header[1] contains number of stream part. */
			this.urls.add(new String(
				Arrays.copyOfRange(header, 2, header.length),
				Charset.forName("UTF-8")
			));
		}
	}
	
	public void channelData(Channel channel, byte[] data){
		/* Ignore */
	}
	
	public void channelEnd(Channel channel){
		Channel.unregister(channel.getId());
		
		this.done.release();
	}
	
	public void channelError(Channel channel){
		this.done.release();
	}
	
	private List<String> getData(){
		/* Return HTTP stream URLs. */
		return this.urls;
	}
	
	public List<String> get(){
		/* Wait for data to become available. */
		this.done.acquireUninterruptibly();

		/* Return HTTP stream URLs. */
		return this.getData();
	}
	
	public List<String> get(long timeout, TimeUnit unit){
		/* Wait for data to become available. */
		try{
			if(!this.done.tryAcquire(timeout, unit)){
				throw new TimeoutException("Timeout while waiting for data.");
			}
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		catch(TimeoutException e){
			throw new RuntimeException(e);
		}

		/* Return HTTP stream URLs. */
		return this.getData();
	}
	
	public boolean isDone(){
		return this.done.availablePermits() > 0;
	}
}
