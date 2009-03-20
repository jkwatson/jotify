package de.felixbruns.jotify;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.Player;
import de.felixbruns.jotify.protocol.Command;
import de.felixbruns.jotify.protocol.CommandListener;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.Session;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelAudioHandler;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.util.GZIP;
import de.felixbruns.jotify.util.XML;
import de.felixbruns.jotify.util.XMLElement;

public class Spotify extends Thread implements CommandListener {
	private Session  session;
	private Protocol protocol;
	private Player   player;
	private boolean  close;
	
	public Spotify(){
		this.session  = new Session();
		this.protocol = null;
		this.player   = new Player();
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
	public Result search(String query){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send search query. */
		this.protocol.sendSearchQuery(callback, query);
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.getData());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		XMLElement resultElement = XML.load(data);
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
	}
	
	/* Request an image. */
	public Image image(String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send image request. */
		this.protocol.sendImageRequest(callback, id);
		
		/* Get data and inflate it. */
		byte[] data = callback.getData();
		
		/* Create Image. */
		try{
			return ImageIO.read(new ByteArrayInputStream(data));
		}
		catch(IOException e){
			return null;
		}
	}
	
	/* Browse something. */
	private XMLElement browse(int type, String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		this.protocol.sendBrowseRequest(callback, type, id);
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.getData());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		return XML.load(data);
	}
	
	/* Browse an artist. */
	public Artist browse(Artist artist){
		/* Browse. */
		XMLElement artistElement = this.browse(1, artist.getId());
		
		/* Create result from XML. */
		return Artist.fromXMLElement(artistElement);
	}
	
	/* Browse an album. */
	public Album browse(Album album){
		/* Browse. */
		XMLElement albumElement = this.browse(2, album.getId());
		
		/* Create result from XML. */
		return Album.fromXMLElement(albumElement);
	}
	
	/* Browse a track. */
	public Result browse(Track track){
		/* Browse. */
		XMLElement resultElement = this.browse(3, track.getId());
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
	}
	
	public void play(Track track){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send play request (token notify + AES key). */
		this.protocol.sendPlayRequest(callback, track);
		
		/* Get AES key. */
		byte[] key = callback.getData();
		
		/* Create piped streams (128 kilobyte buffer). */
		PipedOutputStream output = new PipedOutputStream();
		PipedInputStream  input  = new PipedInputStream(0x20000);
		
		/* Connect piped streams. */
		try{
			output.connect(input);
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		int offset = 0;
		int length = 160 * 1024 * 5 / 8; /* 160 kbit * 5 seconds. */
		
		/* Send substream request. */
		this.protocol.sendSubstreamRequest(new ChannelAudioHandler(key, output), track, offset, length);
		
		/* Play */
		if(this.player.open(input)){
			this.player.play();
		}
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
				byte[] rsaPublicKey = RSA.keyToBytes(this.session.getRSAPublicKey());
				
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
				/* Channel id is at offset 2. AES Key is at offset 4. */
				Channel.process(Arrays.copyOfRange(payload, 2, payload.length));
				
				break;
			}
			case Command.COMMAND_SHAHASH: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_COUNTRYCODE: {
				System.out.println("Country: " + new String(payload));
				
				break;
			}
			case Command.COMMAND_P2P_INITBLK: {
				/* Do nothing. */
				break;
			}
			case Command.COMMAND_NOTIFY: {
				/* HTML-notification, shown in a yellow bar in the official client. */
				/* Skip 11 byte header... */
				System.out.println("Notification: " + new String(
					Arrays.copyOfRange(payload, 11, payload.length)
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				/* Payload is uncompressed XML. */
				if(!new String(payload).contains("<catalogue>premium</catalogue>")){
					System.err.println(
						"Sorry, you need a premium account to use jotify (this is a restriction by Spotify)."
					);
				}
				
				break;
			}
			case Command.COMMAND_WELCOME: {
				/* Request ads. */
				//this.protocol.sendAdRequest(new ChannelAdapter(), 0);
				//this.protocol.sendAdRequest(new ChannelAdapter(), 1);
				
				break;
			}
			case Command.COMMAND_PAUSE: {
				/* TODO */
				break;
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		/* Create a spotify object. */
		Spotify spotify = new Spotify();
		
		spotify.login("username", "password");
		
		spotify.start();
		
		Result result = spotify.search("Clocks");
		
		spotify.play(result.getTracks().get(0));
		
		//Artist artist = spotify.browse(result.getArtists().get(0));
		//artist = result.getArtists().get(0);
		//Image image = spotify.image(artist.getPortrait());
		//ImageIO.write((RenderedImage)image, "JPEG", new File(artist.getPortrait() + ".jpg"));
	}
}
