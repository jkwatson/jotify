package de.felixbruns.jotify.media;

import de.felixbruns.jotify.util.Hex;

/**
 * Holds information about a file.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class File {
	/**
	 * The files 40-character hex identifier.
	 */
	private String id;
	
	/**
	 * The files format. e.g. Ogg Vorbis,320000,...
	 */
	private String format;
	
	/**
	 * Creates an empty {@link File} object.
	 */
	protected File(){
		this.id     = null;
		this.format = null;
	}
	
	/**
	 * Creates a {@link File} object with the specified {@code id} and {@code format}.
	 * 
	 * @param id     Id of the file.
	 * @param format Format of the file.
	 * 
	 * @throws IllegalArgumentException If the given id is invalid.
	 */
	public File(String id, String format){
		/* Check if id string is valid. */
		if(id == null || id.length() != 40 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 40-character hex string.");
		}
		
		/* Set object properties. */
		this.id         = id;
		this.format     = format;
	}
	
	/**
	 * Get the files identifier.
	 * 
	 * @return A 40-character identifier.
	 */
	public String getId(){
		return this.id;
	}
	
	/**
	 * Set the files identifier.
	 * 
	 * @param id A 40-character identifier.
	 * 
	 * @throws IllegalArgumentException If the given id is invalid.
	 */
	public void setId(String id){
		/* Check if id string is valid. */
		if(id == null || id.length() != 40 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 40-character hex string.");
		}
		
		this.id = id;
	}
	
	/**
	 * Get the files format.
	 * 
	 * @return A format string including codec and bitrate.
	 */
	public String getFormat(){
		return this.format;
	}
	
	/**
	 * Set the files format.
	 * 
	 * @param format A format string including codec and bitrate.
	 */
	public void setFormat(String format){
		this.format = format;
	}
	
	/**
	 * Get the files bitrate
	 * 
	 * @return An integer.
	 */
	public int getBitrate(){
		return Integer.parseInt(this.format.split(",")[1]);
	}
}
