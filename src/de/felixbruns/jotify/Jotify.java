package de.felixbruns.jotify;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import de.felixbruns.jotify.crypto.RSA;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.ChannelPlayer;
import de.felixbruns.jotify.protocol.Command;
import de.felixbruns.jotify.protocol.CommandListener;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.Session;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;
import de.felixbruns.jotify.util.GZIP;
import de.felixbruns.jotify.util.XML;
import de.felixbruns.jotify.util.XMLElement;

public class Jotify extends Thread implements CommandListener {
	private Session       session;
	private Protocol      protocol;
	private ChannelPlayer player;
	private boolean       close;
	
	private static Jotify instance;
	
	public static Jotify getInstance(){
		if(instance == null){
			instance = new Jotify();
		}
		
		return instance;
	}
	
	public Jotify(){
		this.session  = new Session();
		this.protocol = null;
		this.player   = null;
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
		
		/* This will make receivePacket return immediately. */
		if(this.protocol != null){
			this.protocol.disconnect();
		}
	}
	
	/* This runs all packet IO stuff in a thread. */
	public void run(){
		if(this.protocol == null){
			System.err.println("You need to login first!");
			
			return;
		}
		
		while(!this.close && this.protocol.receivePacket());
		
		/* Don't call disconnect twice. */
		if(!this.close){
			this.protocol.disconnect();
		}
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
		XMLElement resultElement = XML.load(data, Charset.forName("UTF-8"));
		
		/* Create result from XML. */
		return Result.fromXMLElement(query, resultElement);
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
		return XML.load(data, Charset.forName("UTF-8"));
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
	
	/* Browse tracks. */
	public Result browse(List<Track> tracks){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Create id list. */
		List<String> ids = new ArrayList<String>();
		
		for(Track track : tracks){
			ids.add(track.getId());
		}
		
		/* Send browse request. */
		this.protocol.sendBrowseRequest(callback, 3, ids);
		
		/* Get data and inflate it. */
		byte[] data = GZIP.inflate(callback.getData());
		
		/* Cut off that last 0xFF byte... */
		data = Arrays.copyOfRange(data, 0, data.length - 1);
		
		/* Load XML. */
		XMLElement resultElement = XML.load(data, Charset.forName("UTF-8"));
		
		/* Create result from XML. */
		return Result.fromXMLElement(resultElement);
	}
	
	public Playlist playlist(String id){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		this.protocol.sendPlaylistRequest(callback, id);
		
		/* Get data and inflate it. */
		byte[] data = callback.getData();
		
		/* Load XML. */
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		return Playlist.fromXMLElement(playlistElement, id);
	}
	
	public List<Playlist> playlists(){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send browse request. */
		this.protocol.sendPlaylistRequest(callback, "0000000000000000000000000000000000");
		
		/* Get data and inflate it. */
		byte[] data = callback.getData();
		
		/* Load XML. */
		XMLElement playlistElement = XML.load(
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
			new String(data, Charset.forName("UTF-8")) +
			"</playlist>"
		);
		
		return Playlist.listFromXMLElement(playlistElement);
	}
	
	public void play(){
		if(this.player != null){
			this.player.play();
		}
	}
	
	public void pause(){
		if(this.player != null){
			this.player.stop();
		}
	}
	
	public void stopPlay(){
		if(this.player != null){
			this.player.close();
		}
	}
	
	public Track track(){
		if(this.player != null){
			return this.player.track();
		}
		
		return null;
	}
	
	public int position(){
		if(this.player != null){
			return this.player.position();
		}
		
		return -1;
	}
	
	public void play(Track track){
		/* Create channel callback */
		ChannelCallback callback = new ChannelCallback();
		
		/* Send play request (token notify + AES key). */
		this.protocol.sendPlayRequest(callback, track);
		
		/* Get AES key. */
		byte[] key = callback.getData();
		
		/* Create channel player. */
		this.player = new ChannelPlayer(this.protocol, track, key);
		
		/* Start playing. */
		this.play();
	}
	
	/* Handle incoming commands. */
	public void commandReceived(int command, byte[] payload){
		//System.out.format("< Command: 0x%02x Length: %d\n", command, payload.length);
		
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
				System.out.println("Country: " + new String(payload, Charset.forName("UTF-8")));
				
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
					Arrays.copyOfRange(payload, 11, payload.length), Charset.forName("UTF-8")
				));
				
				break;
			}
			case Command.COMMAND_PRODINFO: {
				/* Payload is uncompressed XML. */
				if(!new String(payload, Charset.forName("UTF-8")).contains("<type>premium</type>")){
					System.err.println(
						"Sorry, you need a premium account to use jotify (this is a restriction by Spotify)."
					);
					
					System.exit(0);
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
		Jotify spotify = Jotify.getInstance();
		
		spotify.login("username", "password");
		
		/* Start packet IO in the background. */
		spotify.start();
		
		/* Get a list of this users playlists. */
		//List<Playlist> playlists = spotify.playlists();
		
		//spotify.playlist(playlists.get(0).getId());
		
		/* Search for an artist / album / track. */
		//Result result = spotify.search("Coldplay");
		
		/* Play first track in result. */
		//spotify.play(result.getTracks().get(0));
		
		/* Browse */
		//Artist artist = spotify.browse(result.getArtists().get(0));
		
		/* Load an image and save it. */
		//Image image = spotify.image(artist.getPortrait());
		//ImageIO.write((RenderedImage)image, "JPEG", new File(artist.getPortrait() + ".jpg"));
	}
}
