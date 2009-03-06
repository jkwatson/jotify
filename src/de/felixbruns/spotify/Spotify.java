package de.felixbruns.spotify;

import de.felixbruns.spotify.crypto.RSA;
import de.felixbruns.spotify.util.Buffer;
import de.felixbruns.spotify.util.GZIP;

public class Spotify extends Thread implements CommandListener, ChannelListener {
	private Session  session;
	private Protocol protocol;
	private boolean  close;
	private boolean  dataAvailable;
	private byte[]   data;
	
	public Spotify(){
		this.session  = new Session();
		this.protocol = null;
		this.close    = false;
	}
	
	/* Login to Spotify. */
	public boolean login(String username, String password){
		/* Authenticate session. */
		this.protocol = this.session.authenticate(username, password);
		
		if(this.protocol == null){
			return false;
		}
		
		/* Add command handler. */
		this.protocol.addListener(this);
		
		return true;
	}
	
	/* Closes Spotify connection. */
	public void close(){
		this.close = true;
	}
	
	/* This runs all packet IO stuff in a thread. */
	public void run(){
		if(this.protocol == null){
			System.err.println("You need to login first!");
			
			return;
		}
		
		while(!close && this.protocol.receivePacket());
		
		this.protocol.disconnect();
	}
	
	/* Search for something. */
	public byte[] search(String query){
		this.dataAvailable = false;
		
		/* Send search query. */
		this.protocol.sendSearchQuery(new ChannelAdapter(){
			private Buffer buffer = new Buffer();
			
			public void channelData(Channel channel, byte[] data){
				this.buffer.appendBytes(data);
			}
			
			public void channelEnd(Channel channel){
				data          = GZIP.inflate(this.buffer.getBytes());
				dataAvailable = true;
			}
		}, query);
		
		/* Wait for data to become available. */
		while(!this.dataAvailable){
			try{
				Thread.sleep(10);
			}
			catch(InterruptedException e){}
		}
		
		return this.data;
	}
	
	/* Handle incoming commands. */
	public void commandReceived(int command, byte[] payload){
		System.out.format("Command: 0x%02x Length: %d\n", command, payload.length);
		
		switch(command){
			case Command.COMMAND_SECRETBLK: {
				/* Check length. */
				if(payload.length != 336){
					System.err.format("Got command 0x02 with len %d, expected 336!\n", payload.length);
				}
				
				/* Check RSA public key. */
				byte[] rsaPublicKey = RSA.keyToBytes(this.session.rsaClientKeyPair.getPublicKey());
				
				for(int i = 0; i < 128; i++){
					if(payload[16 + i] != rsaPublicKey[i]){
						System.err.format("RSA public key doesn't match! %d\n", i);
						
						break;
					}
				}
				
				/* Send cache hash. */
				this.protocol.sendCacheHash();
				
				break;
			}
			case Command.COMMAND_PING: {
				/* Ignore the timestamp but respond to the request. */
				/* int timestamp = IntegerUtilities.bytesToInteger(payload); */
				this.protocol.sendPong();
				
				break;
			}
			case Command.COMMAND_CHANNELDATA: {
				Channel.process(payload);
				
				break;
			}
			case Command.COMMAND_CHANNELERR: {
				Channel.error(payload);
				
				break;
			}
			case Command.COMMAND_AESKEY: {
				/*channelId = ShortUtilities.bytesToUnsignedShort(payload, 2);
				
				System.out.format("AES key for channel %d\n", channelId);*/
				/* Key: Arrays.copyOfRange(payload, 4, payload.length - 4);<*/
				break;
			}
			case Command.COMMAND_SHAHASH: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_COUNTRYCODE: {
				/* Do nothing. */
				//System.out.println(new String(payload));
				break;
			}
			case Command.COMMAND_P2P_INITBLK: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip header. */
				/*System.out.println(new String(
					Arrays.copyOfRange(payload, 11, payload.length)
				));*/
				break;
			}
			case Command.COMMAND_PRODINFO: {
				/* Payload is uncompressed XML. */
				//System.out.println(new String(payload));
				break;
			}
			case Command.COMMAND_WELCOME: {
				/* Request ads. */
				this.protocol.sendAdRequest(this, 0);
				this.protocol.sendAdRequest(this, 1);
				break;
			}
			case Command.COMMAND_PAUSE: {
				/* TODO */
				break;
			}
		}
	}
	
	public void channelData(Channel channel, byte[] data){
		/*if(this.buffers.containsKey(channel.getId())){
			this.buffers.get(channel.getId()).appendBytes(data);
		}*/
	}
	
	public void channelEnd(Channel channel){
		/*if(this.buffers.containsKey(channel.getId())){
			byte[] bytes = this.buffers.remove(channel.getId()).getBytes();
		}*/
	}
	
	public void channelError(Channel channel){
		/*if(this.buffers.containsKey(channel.getId())){
			this.buffers.remove(channel.getId());
		}*/
	}
	
	public void channelHeader(Channel channel, byte[] header){
		/*if(!this.buffers.containsKey(channel.getId())){
			this.buffers.put(channel.getId(), new Buffer());
		}*/
	}
	
	public static void main(String[] args){
		/* Create a spotify object. */
		Spotify spotify = new Spotify();
		
		spotify.login("username", "password");
		
		spotify.start();
		
		System.out.println(new String(spotify.search("Coldplay")));
	}
}
