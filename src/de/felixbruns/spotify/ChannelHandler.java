package de.felixbruns.spotify;

import java.util.HashMap;
import java.util.Map;

import de.felixbruns.spotify.util.Buffer;

public class ChannelHandler implements ChannelListener {
	private Map<Integer, Buffer> buffers;
	
	public ChannelHandler(){
		this.buffers = new HashMap<Integer, Buffer>();
	}
	
	public void channelData(Channel channel, byte[] data){
		if(this.buffers.containsKey(channel.getId())){
			this.buffers.get(channel.getId()).appendBytes(data);
		}
	}
	
	public void channelEnd(Channel channel){
		if(this.buffers.containsKey(channel.getId())){
			this.buffers.remove(channel.getId()).getBytes();
		}
	}
	
	public void channelError(Channel channel){
		if(this.buffers.containsKey(channel.getId())){
			this.buffers.remove(channel.getId());
		}
	}
	
	public void channelHeader(Channel channel, byte[] header){
		if(!this.buffers.containsKey(channel.getId())){
			this.buffers.put(channel.getId(), new Buffer());
		}
	}
}
