package de.felixbruns.jotify.media;

import java.util.Properties;

public class User {
	private String     name;
	private String     country;
	private String     notification;
	private Properties properties;
	
	public User(String name){
		this(name, null, null);
	}
	
	public User(String name, String country, String type){
		this.name         = name;
		this.country      = country;
		this.notification = null;
		this.properties   = new Properties();
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
	
	public boolean isPremium(){
		return this.properties.getProperty("type").equals("premium");
	}
	
	public String getNotification(){
		return this.notification;
	}
	
	public void setNotification(String notification){
		this.notification = notification;
	}
	
	public String getProperty(String key){
		return this.properties.getProperty(key);
	}
	
	public void setProperty(String key, String value){
		this.properties.setProperty(key, value);
	}
	
	public String toString(){
		return String.format("[User: %s, %s, %s]", this.name, this.country, this.properties.getProperty("type"));
	}
}
