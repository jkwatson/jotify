package de.felixbruns.jotify.media;

import de.felixbruns.jotify.util.XMLElement;

public class User {
	private String name;
	private String country;
	private String type;
	private String notification;
	
	public User(String name){
		this(name, null, null);
	}
	
	public User(String name, String country, String type){
		this.name         = name;
		this.country      = country;
		this.type         = type;
		this.notification = null;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getCountry(){
		return this.country;
	}
	
	public void setCountry(String country){
		this.country = country;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	public boolean isPremium(){
		return this.type.equals("premium");
	}
	
	public String getNotification(){
		return this.notification;
	}
	
	public void setNotification(String notification){
		this.notification = notification;
	}
	
	public static User fromXMLElement(XMLElement prodinfoElement, User user){
		/* Get "product" element. */
		XMLElement productElement = prodinfoElement.getChild("product");
		
		/* Set type. */
		user.type = productElement.getChildText("type");
		
		return user;
	}
	
	public String toString(){
		return String.format("[User: %s, %s, %s]", this.name, this.country, this.type);
	}
}
