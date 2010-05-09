package de.felixbruns.jotify.async;

import java.util.List;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.User;
import de.felixbruns.jotify.player.PlaybackAdapter;

/**
 * A small commandline Spotify client using the asynchronous jotify API.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Example
 */
public class AsyncJotifyExample extends AsyncJotifyAdapter implements Runnable {
	private AsyncJotifyConnection jotify;
	private List<Track>           tracks;
	private int                   position;
	private Scanner               scanner;
	
	public AsyncJotifyExample(){
		/* Create a connection to Spotify. */
		this.jotify = new AsyncJotifyConnection();
		
		/* Current list of tracks. */
		this.tracks = null;
		
		/* Current track. */
		this.position = -1;
		
		/* Scanner to read user input. */
		this.scanner = new Scanner(System.in);
		
		/* Read username and password. */
		System.out.print("Username: ");
		String username = scanner.nextLine();
		
		System.out.print("Password: ");
		String password = scanner.nextLine();
		
		/* Add self to listeners. */
		this.jotify.addListener(this);
		
		/* Login. */
		this.jotify.login(username, password);
	}
	
	public void loggedIn(){
		/*
		 * Start main program loop in a thread.
		 * This is important! Otherwise we would
		 * block the jotify I/O-thread.
		 */
		new Thread(this).start();
	}
	
	public void loggedOut(){
		/* Show a message here... */
	}
	
	public void run(){
		/* Pattern for commands. */
		Pattern pattern = Pattern.compile("([a-z]+)\\s*(.*)", Pattern.CASE_INSENSITIVE);
		String  line;
		Matcher matcher;
		String  command;
		String  argument;

		/* Wait for commands. */
		while(true){
			try{
				line     = this.scanner.nextLine();
				matcher  = pattern.matcher(line);
				command  = null;
				argument = null;
				
				/* Check if command is valid. */
				if(matcher.matches()){
					command = matcher.group(1);
					
					if(matcher.groupCount() == 2){
						argument = matcher.group(2);
					}
				}
				else{
					command = "";
				}
				
				/* Search. */
				if(command.equals("search")){
					this.jotify.search(argument);
				}
				/* Playlist. */
				else if(command.equals("playlist")){
					this.jotify.requestPlaylist(argument);
				}
				/* Toplist. */
				else if(command.equals("toplist")){
					this.jotify.requestToplist(argument, null, null);
				}
				/* Play. */
				else if(command.equals("play")){
					if(argument.isEmpty()){
						this.jotify.play();
					}
					else{
						this.position = Integer.parseInt(argument);
						
						this.play(this.position);
					}
				}
				/* Pause. */
				else if(command.equals("pause")){
					this.jotify.pause();
				}
				/* Stop. */
				else if(command.equals("stop")){
					this.jotify.stop();
				}
				/* Seek. */
				else if(command.equals("seek")){
					int seconds = Integer.parseInt(argument);
					
					this.jotify.seek(seconds * 1000);
				}
				else if(command.equals("quit")){
					/* Stop playing. */
					this.jotify.stop();
					
					/* Close connection. */
					this.jotify.logout();
					
					break;
				}
				else{
					System.out.println("Commands:");
					System.out.println("	search   <query>");
					System.out.println("	playlist <id/uri>");
					System.out.println("	toplist  <type>");
					System.out.println("	play     [<position>]");
					System.out.println("	pause");
					System.out.println("	stop");
					System.out.println("	seek     <seconds>");
					System.out.println("	quit");
				}
			}
			catch(Exception e){
				e.printStackTrace();
				
				System.out.format("Error: %s\n", e.getMessage());
			}
		}
	}
	
	public void receivedUserData(User user){
		System.out.println(user);
		
		/* Check if user has a premium account. */
		if(!user.isPremium()){
			System.err.println("\n\nWARNING: You don't have a premium account! Jotify will NOT work!\n");
		}
	}
	
	public void receivedSearchResult(Result result, Object userdata){
		/* Check result. */
		if(result.getSuggestion() != null){
			System.out.format("Did you mean: '%s'?\n", result.getSuggestion());
		}
		
		/* Set current track list. */
		this.tracks = result.getTracks();
		
		if(this.tracks.size() == 0){
			System.out.println("Nothing found...");
		}
		
		/* Display 10 tracks. */
		for(int i = 0; i < 10 && i < this.tracks.size(); i++){
			System.out.format(
				"%2d | %20s - %45s | %32s\n", i,
				tracks.get(i).getArtist().getName(),
				tracks.get(i).getTitle(),
				tracks.get(i).getId()
			);
		}
	}
	
	public void receivedPlaylist(Playlist playlist){
		/* Browse tracks to get metadata, then set current track list. */
		this.jotify.browse(playlist.getTracks());
	}
	
	public void receivedTracks(List<Track> tracks, Object userdata) {
		/* Display tracks. */
		int i = 0;
		
		this.tracks = tracks;
		
		for(Track track : tracks){
			System.out.format(
				"%2d | %20s - %45s | %32s\n", i,
				track.getArtist().getName(),
				track.getTitle(), track.getId()
			);
			
			i++;
		}
	}
	
	public void receivedToplist(Result toplist, Object userdata){
		/* Set current track list. */
		this.tracks = toplist.getTracks();
		
		/* Display 10 artists. */
		for(int i = 0; i < 10 && i < toplist.getArtists().size(); i++){
			System.out.format(
				"%2d | %20s | %32s\n", i,
				toplist.getArtists().get(i).getName(),
				toplist.getArtists().get(i).getId()
			);
		}
		
		/* Display 10 albums. */
		for(int i = 0; i < 10 && i < toplist.getAlbums().size(); i++){
			System.out.format(
				"%2d | %20s - %45s | %32s\n", i,
				toplist.getAlbums().get(i).getArtist().getName(),
				toplist.getAlbums().get(i).getName(),
				toplist.getAlbums().get(i).getId()
			);
		}
		
		/* Display 10 tracks. */
		for(int i = 0; i < 10 && i < this.tracks.size(); i++){
			System.out.format(
				"%2d | %20s - %45s | %32s\n", i,
				tracks.get(i).getArtist().getName(),
				tracks.get(i).getTitle(),
				tracks.get(i).getId()
			);
		}
	}
	
	public void play(int pos) throws Exception {
		/* Check position in current track list. */
		if(pos >= 0 && pos < this.tracks.size()){
			/* Stop if something is already playing. */
			this.jotify.stop();
			
			/* Get current track. */
			Track track = this.tracks.get(pos);
			
			/* Print artist and title. */
			System.out.format(
				"Playing: %s - %s\n",
				track.getArtist().getName(),
				track.getTitle()
			);
			
			/* Start playing. */
			this.jotify.play(track, File.BITRATE_160, new PlaybackAdapter(){
				public void playbackFinished(Track track){
					try{
						/* Play next track. */
						play(++position);
					}
					catch(Exception e){
						System.out.format("Error: %s\n", e.getMessage());
					}
				}
			});
		}
		else{
			System.out.format("Position '%d' not available in current track list!\n", this.position);
		}
	}
	
	public static void main(String[] args) throws Exception {
		new AsyncJotifyExample();
	}
}
