package de.felixbruns.jotify.gui.util;

public class JotifyPreferences extends Preferences {
	private static JotifyPreferences instance;
	
	static {
		instance = new JotifyPreferences(
			System.getProperty("user.home") + "/.jotify-settings.xml"
		);
	}
	
	public static JotifyPreferences getInstance(){
		return instance;
	}
	
	private JotifyPreferences(String file){
		super(file);
	}

	public JotifyLoginCredentials getLoginCredentials(){
		/* Check if login credentials are present. */
		if(this.contains("login.username") &&
			this.contains("login.password") &&
			this.contains("login.remember")){
			/* Construct object and return it. */
			return new JotifyLoginCredentials(
				this.getString("login.username"),
				this.getString("login.password"),
				this.getBoolean("login.remember")
			);
		}
		
		/* Return empty login credentials otherwise. */
		return JotifyLoginCredentials.emptyLoginCredentials();
	}
	
	public void setLoginCredentials(JotifyLoginCredentials credentials){
		/* Set values. */
		this.setString("login.username", credentials.getUsername());
		this.setString("login.password", credentials.getPassword());
		this.setBoolean("login.remember", credentials.getRemember());
	}
	
	public long getPlaylistsRevision(){
		return this.getLong("playlists.revision", -1);
	}
	
	public void setPlaylistsRevision(long revision){
		this.setLong("playlists.revision", revision);
	}
}
