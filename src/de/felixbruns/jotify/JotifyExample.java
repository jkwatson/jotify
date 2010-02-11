package de.felixbruns.jotify;

import java.util.List;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.felixbruns.jotify.exceptions.AuthenticationException;

import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.User;
import de.felixbruns.jotify.player.PlaybackAdapter;

/**
 * A small commandline Spotify client using jotify.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 * 
 * @category Example
 */
public class JotifyExample {
	/* Create a connection to Spotify. */
	private static JotifyConnection jotify = new JotifyConnection();
	
	/* Current list of tracks. */
	private static List<Track> tracks = null;
	
	/* Current track. */
	private static int position = -1;
	
	/**
	 * Main method.
	 * 
	 * @param args Commandline arguments.
	 * 
	 * @throws Exception Because I don't want to catch all of them :-P
	 */
	public static void main(String[] args) throws Exception {
		/* Create a scanner to read user input. */
		Scanner scanner = new Scanner(System.in);
		
		/* Login. */
		while(true){
			System.out.print("Username: ");
			String username = scanner.nextLine();
			
			System.out.print("Password: ");
			String password = scanner.nextLine();
			
			try{
				jotify.login(username, password);
				
				System.out.println("Logged in! Press enter to see available commands.\n");
				
				break;
			}
			catch(AuthenticationException e){
				System.out.println("Invalid username and/or password! Try again.\n");
			}
		}
		
		/* Get and print some user info. */
		User user = jotify.user();
		
		System.out.println(user);
		
		/* Check if user has a premium account. */
		if(!user.isPremium()){
			System.err.println("\n\nWARNING: You don't have a premium account! Jotify will NOT work!\n");
		}
		
		/* Wait for commands. */
		Pattern pattern = Pattern.compile("([a-z]+)\\s*(.*)", Pattern.CASE_INSENSITIVE);
		
		while(true){
			try{
				String   line     = scanner.nextLine();
				Matcher  matcher  = pattern.matcher(line);
				String   command  = null;
				String   argument = null;
				
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
					Result result = jotify.search(argument);
					
					if(result == null){
						throw new Exception("Search failed!");
					}
					
					/* Check result. */
					if(result.getSuggestion() != null){
						System.out.format("Did you mean: '%s'?\n", result.getSuggestion());
					}
					
					/* Set current track list. */
					tracks = result.getTracks();
					
					if(tracks.size() == 0){
						System.out.println("Nothing found...");
					}
					
					/* Display 10 tracks. */
					for(int i = 0; i < 10 && i < tracks.size(); i++){
						System.out.format(
							"%2d | %20s - %45s | %32s\n", i,
							tracks.get(i).getArtist().getName(),
							tracks.get(i).getTitle(),
							tracks.get(i).getId()
						);
					}
				}
				/* Playlist. */
				else if(command.equals("playlist")){
					Playlist playlist = jotify.playlist(argument);
					
					if(playlist == null){
						throw new Exception("Playlist loading failed!");
					}
					
					/* 
					 * Browse tracks to get metadata,
					 * then set current track list.
					 */
					tracks = jotify.browse(playlist.getTracks());
					
					if(tracks == null){
						throw new Exception("Browsing playlist tracks failed!");
					}
					
					/* Display tracks. */
					int i = 0;
					
					for(Track track : tracks){
						System.out.format(
							"%2d | %20s - %45s | %32s\n", i,
							track.getArtist().getName(),
							track.getTitle(), track.getId()
						);
						
						i++;
					}
				}
				/* Toplist. */
				else if(command.equals("toplist")){
					Result result = jotify.toplist(argument, null, null);
					
					if(result == null){
						throw new Exception("Toplist request failed!");
					}
					
					/* Set current track list. */
					tracks = result.getTracks();
					
					/* Display 10 artists. */
					for(int i = 0; i < 10 && i < result.getArtists().size(); i++){
						System.out.format(
							"%2d | %20s | %32s\n", i,
							result.getArtists().get(i).getName(),
							result.getArtists().get(i).getId()
						);
					}
					
					/* Display 10 albums. */
					for(int i = 0; i < 10 && i < result.getAlbums().size(); i++){
						System.out.format(
							"%2d | %20s - %45s | %32s\n", i,
							result.getAlbums().get(i).getArtist().getName(),
							result.getAlbums().get(i).getName(),
							result.getAlbums().get(i).getId()
						);
					}
					
					/* Display 10 tracks. */
					for(int i = 0; i < 10 && i < tracks.size(); i++){
						System.out.format(
							"%2d | %20s - %45s | %32s\n", i,
							tracks.get(i).getArtist().getName(),
							tracks.get(i).getTitle(),
							tracks.get(i).getId()
						);
					}
				}
				/* Play. */
				else if(command.equals("play")){
					position = Integer.parseInt(argument);
					
					play(position);
				}
				else if(command.equals("quit")){
					/* Stop playing. */
					jotify.stop();
					
					/* Close connection. */
					jotify.close();
					
					break;
				}
				else{
					System.out.println("Commands:");
					System.out.println("	search   <query>");
					System.out.println("	playlist <id/uri>");
					System.out.println("	toplist  <type>");
					System.out.println("	play     <position>");
					System.out.println("	quit");
				}
			}
			catch(Exception e){
				System.out.format("Error: %s\n", e.getMessage());
			}
		}
	}
	
	public static void play(int i) throws Exception {
		/* Check position in current track list. */
		if(i >= 0 && i < tracks.size()){
			/* Stop if something is already playing. */
			jotify.stop();
			
			/* Load metadata (files etc.) for track. */
			Track track = jotify.browse(tracks.get(i));		
			
			if(track == null){
				throw new Exception("Browsing track failed!");
			}
			
			/* Print artist and title. */
			System.out.format(
				"Playing: %s - %s\n",
				track.getArtist().getName(),
				track.getTitle()
			);
			
			/* Start playing (without a PlaybackListener). */
			jotify.play(track, new PlaybackAdapter(){
				public void playbackFinished(Track track){
					try{
						play(++position);
					}
					catch(Exception e){
						System.out.format("Error: %s\n", e.getMessage());
					}
				}
			});
		}
		else{
			System.out.format("Position '%d' not available in current track list!\n", position);
		}
	}
}
