package de.felixbruns.jotify.gui.util;

public class JotifyLoginCredentials {
	private String  username;
	private String  password;
	private boolean remember;
	
	public JotifyLoginCredentials(String username, char[] password){
		this(username, password, false);
	}
	
	public JotifyLoginCredentials(String username, char[] password, boolean remember){
		this(username, new String(password), remember);
	}
	
	public JotifyLoginCredentials(String username, String password){
		this(username, password, false);
	}
	
	public JotifyLoginCredentials(String username, String password, boolean remember){
		this.username = username;
		this.password = password;
		this.remember = remember;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	public void setPassword(char[] password){
		this.password = new String(password);
	}
	
	public void setPassword(String password){
		this.password = password;
	}
	
	public boolean getRemember(){
		return this.remember;
	}
	
	public void setRemember(boolean remember){
		this.remember = remember;
	}
	
	public static JotifyLoginCredentials emptyLoginCredentials(){
		return new JotifyLoginCredentials(null, (String)null);
	}
}
