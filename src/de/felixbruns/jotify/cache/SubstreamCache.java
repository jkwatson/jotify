package de.felixbruns.jotify.cache;

import java.io.File;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;

/**
 * A special {@link FileCache} for storing and retrieving audio substreams.
 * Introduces a {@code hash} method and an asynchronous {@code load} method.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class SubstreamCache extends FileCache {
	/**
	 * Create a new {@link SubstreamCache} with a default directory.
	 * The directory will be '$HOME/.jotify-cache'.
	 */
	public SubstreamCache(){
		super();
	}
	
	/**
	 * Create a new {@link SubstreamCache} with a specified directory.
	 * If the directory doesn't exist, it will be created.
	 * 
	 * @param directory The directory to use for storing substream data.
	 */
	public SubstreamCache(File directory){
		super(directory);
	}
	
	/**
	 * Create a hash for a specified {@link Track}, {@code offset} and {@code length}.
	 * 
	 * @param track  A {@link Track} object.
	 * @param offset An integer denoting the offset of the substream.
	 * @param length An integer denoting the length of the substream.
	 * 
	 * @return A hash for the specified parameters.
	 */
	public String hash(Track track, int offset, int length){
		return track.getFiles().get(0).getId() + "/" + track.getFiles().get(0).getId() + "-" + offset + "-" + length;
	}
	
	/**
	 * Asynchronously load cached substream data and supply the it via a {@link ChannelListener}.
	 * 
	 * @param category The cache category to load from.
	 * @param hash     The hash of the item to load.
	 * @param listener The {@link ChannelListener} that listens for substream data.
	 */
	public void load(final String category, final String hash, final ChannelListener listener){
		/* Load data in a separate thread, because we're an asynchronous load method. */
		new Thread("Cached-Substream-Channel-Thread"){
			public void run(){
				/* Create a new channel. */
				Channel channel = new Channel(
					"Cached-Substream-Channel", Channel.Type.TYPE_SUBSTREAM, null
				);
				
				/* Supply data to listener. */
				listener.channelHeader(channel, null);
				listener.channelData(channel, load(category, hash));
				listener.channelEnd(channel);
			}
		}.start();
	}
}
