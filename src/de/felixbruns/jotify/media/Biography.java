package de.felixbruns.jotify.media;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds biographical information about an artist.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Media
 */
public class Biography {
	/**
	 * A Biographical text.
	 */
	private String text;
	
	/**
	 * A list of portrait images.
	 */
	private List<Image> portraits;
	
	/**
	 * Creates an empty {@link Biography} object.
	 */
	public Biography(){
		this.text      = null;
		this.portraits = new ArrayList<Image>();
	}
	
	/**
	 * Creates a {@link Biography} object with the specified {@code text}.
	 * 
	 * @param text A Biographical text.
	 */
	public Biography(String text){
		this.text      = text;
		this.portraits = new ArrayList<Image>();
	}
	
	/**
	 * Get the biographical text.
	 * 
	 * @return A Biographical text.
	 */
	public String getText(){
		return this.text;
	}
	
	/**
	 * Get the biographical text.
	 * 
	 * @param text A Biographical text.
	 */
	public void setText(String text){
		this.text = text;
	}
	
	/**
	 * Get a list of portraits.
	 * 
	 * @return A {@link List} of {@link Image} objects.
	 */
	public List<Image> getPortraits(){
		return this.portraits;
	}
	
	/**
	 * Set portraits for this biography.
	 * 
	 * @param portraits A {@link List} of {@link Image} objects.
	 */
	public void setPortraits(List<Image> portraits){
		this.portraits = portraits;
	}
}
