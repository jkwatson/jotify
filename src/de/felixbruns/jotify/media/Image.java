package de.felixbruns.jotify.media;

import de.felixbruns.jotify.util.Hex;

public class Image {
	/**
	 * The images 40-character hex identifier.
	 */
	private String id;
	
	/**
	 * The images width.
	 */
	private int width;
	
	/**
	 * The images height.
	 */
	private int height;
	
	/**
	 * Create an empty {@link Image} object.
	 */
	public Image(){
		this.id     = null;
		this.width  = -1;
		this.height = -1;
	}
	
	/**
	 * Create a new image with the given {@code id}.
	 * 
	 * @param id The images identifier.
	 * 
	 * @throws IllegalArgumentException If the given id is invalid.
	 */
	public Image(String id){
		this(id, -1, -1);
	}
	
	/**
	 * Create a new image with the given {@code id}, {@code width} and {@code height}.
	 * 
	 * @param id     The images identifier.
	 * @param width  The images width.
	 * @param height The images height.
	 * 
	 * @throws IllegalArgumentException If the given id is invalid.
	 */
	public Image(String id, int width, int height){
		/* Check if image id string is valid. */
		if(id == null || id.length() != 40 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 40-character hex string.");
		}
		
		this.id     = id;
		this.width  = width;
		this.height = height;
	}
	
	/**
	 * Get the images identifier.
	 * 
	 * @return A 40-character identifier.
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Set the images identifier.
	 * 
	 * @param id A 40-character identifier.
	 * 
	 * @throws IllegalArgumentException If the given id is invalid.
	 */
	public void setId(String id){
		/* Check if image id string is valid. */
		if(id == null || id.length() != 40 || !Hex.isHex(id)){
			throw new IllegalArgumentException("Expecting a 40-character hex string.");
		}
		
		this.id = id;
	}
	
	/**
	 * Get the images width.
	 * 
	 * @return An integer.
	 */
	public int getWidth(){
		return this.width;
	}
	
	/**
	 * Set the images width.
	 * 
	 * @param width The images width.
	 */
	public void setWidth(int width){
		this.width = width;
	}
	
	/**
	 * Get the images height.
	 * 
	 * @return An integer.
	 */
	public int getHeight() {
		return this.height;
	}
	
	/**
	 * Set the images height.
	 * 
	 * @param height The images height.
	 */
	public void setHeight(int height){
		this.height = height;
	}
}
