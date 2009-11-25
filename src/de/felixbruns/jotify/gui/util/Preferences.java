package de.felixbruns.jotify.gui.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Preferences {
	private Properties properties;
	private String     file;
	
	public Preferences(String file){
		this.properties = new Properties();
		this.file       = file;
	}
	
	public boolean contains(String key){
		return this.properties.containsKey(key);
	}
	
	public void setString(String key, String value){
		this.properties.setProperty(key, value.trim());
	}

	public void setInteger(String key, int value){
		this.properties.setProperty(key, Integer.toString(value));
	}
	
	public void setLong(String key, long value){
		this.properties.setProperty(key, Long.toString(value));
	}
	
	public void setBoolean(String key, boolean value){
		this.properties.setProperty(key, Boolean.toString(value));
	}
	
	public String getString(String key){
		return this.properties.getProperty(key);
	}
	
	public int getInteger(String key){
		return Integer.parseInt(this.properties.getProperty(key));
	}
	
	public long getLong(String key){
		return Long.parseLong(this.properties.getProperty(key));
	}
	
	public boolean getBoolean(String key){
		return Boolean.parseBoolean(this.properties.getProperty(key));
	}
	
	public String getString(String key, String defaultValue){
		return this.properties.getProperty(key, defaultValue);
	}
	
	public int getInteger(String key, int defaultValue){
		return Integer.parseInt(this.properties.getProperty(key, Integer.toString(defaultValue)));
	}
	
	public long getLong(String key, long defaultValue){
		return Long.parseLong(this.properties.getProperty(key, Long.toString(defaultValue)));
	}
	
	public boolean getBoolean(String key, boolean defaultValue){
		return Boolean.parseBoolean(this.properties.getProperty(key, Boolean.toString(defaultValue)));
	}
	
	public boolean load(){
		try{
			FileInputStream input = new FileInputStream(this.file);
			
			this.properties.loadFromXML(input);
		}
		catch(IOException e){
			return false;
		}
		
		return true;
	}
	
	public boolean save(){
		try{
			FileOutputStream output = new FileOutputStream(this.file);
			
			this.properties.storeToXML(output, null);
			
			output.close();
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
