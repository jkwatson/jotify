package de.felixbruns.jotify.media.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistConfirmation;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Track;

public class XMLPlaylistParser extends XMLParser implements XMLStreamConstants {
	/**
	 * Create a new stream parser from the given input stream.
	 * 
	 * @param stream An {@link InputStream} stream to parse.
	 */
	private XMLPlaylistParser(InputStream stream, String encoding) throws XMLStreamException {
		super(stream, encoding);
	}
	
	/**
	 * Parse the input stream as one of {@link PlaylistContainer} or
	 * {@link Playlist}, depending on the document element.
	 * 
	 * @return An {@link Object} which can then be cast.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLParserException
	 */
	private Object parse(String id) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Check if reader is currently on a start element. */
		if(this.reader.getEventType() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Check current element name and start parsing it. */
			if(name.equals("playlists")){
				return this.parsePlaylistContainer();
			}
			else if(name.equals("playlist")){
				return this.parsePlaylist(id);
			}
			else if(name.equals("confirm")){
				return this.parsePlaylistConfirmation();
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		throw new IllegalStateException("Reader is not on a start element!");
	}
	
	private PlaylistContainer parsePlaylistContainer() throws XMLStreamException, XMLParserException {
		PlaylistContainer playlists = new PlaylistContainer();
		String            name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("base-version")){
				this.skipBaseVersion();
			}
			else if(name.equals("next-change")){
				this.parseNextChange(playlists);
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return playlists;
	}
	
	private Playlist parsePlaylist(String id) throws XMLStreamException, XMLParserException {
		Playlist playlist = new Playlist();
		String   name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("base-version")){
				this.skipBaseVersion();
			}
			else if(name.equals("next-change")){
				this.parseNextChange(playlist);
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		playlist.setId(id);
		
		return playlist;
	}
	
	private void skipBaseVersion() throws XMLStreamException, XMLParserException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("rid")){
				this.getElementString(); /* Skip. */
			}
			else if(name.equals("version")){
				this.getElementString(); /* Skip. */
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
	}
	
	private void parseNextChange(Object object) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("change")){
				this.parseChange(object);
			}
			else if(name.equals("rid")){
				this.getElementString(); /* Skip. */
			}
			else if(name.equals("version")){
				String[] parts = this.getElementString().split(",");
				
				if(object instanceof Playlist){
					Playlist playlist = (Playlist)object;
					
					playlist.setRevision(Long.parseLong(parts[0]));
					playlist.setChecksum(Long.parseLong(parts[2]));
					playlist.setCollaborative(Integer.parseInt(parts[3]) == 1);
				}
				else if(object instanceof PlaylistContainer){
					PlaylistContainer playlists = (PlaylistContainer)object;
					
					playlists.setRevision(Long.parseLong(parts[0]));
					playlists.setChecksum(Long.parseLong(parts[2]));
				}
				else{
					throw new XMLParserException(
						"Unexpected object '" + object + "'", this.reader.getLocation()
					);
				}
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
	}
	
	private void parseChange(Object object) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("ops")){
				this.parseOps(object);
			}
			else if(name.equals("time")){
				this.getElementString(); /* Skip. */
			}
			else if(name.equals("user")){
				if(object instanceof Playlist){
					((Playlist)object).setAuthor(this.getElementString());
				}
				else if(object instanceof PlaylistContainer){
					((PlaylistContainer)object).setAuthor(this.getElementString());
				}
				else{
					throw new XMLParserException(
						"Unexpected object '" + object + "'", this.reader.getLocation()
					);
				}
				
				/* Skip characters. */
				this.reader.next();
			}
			else{				
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
	}
	
	private void parseOps(Object object) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("create")){
				this.getElementString(); /* Skip. */
			}
			else if(name.equals("pub") && object instanceof Playlist){
				((Playlist)object).setCollaborative(this.getElementInteger() == 1);
			}
			else if(name.equals("name") && object instanceof Playlist){
				((Playlist)object).setName(this.getElementString());
			}
			else if(name.equals("description") && object instanceof Playlist){
				((Playlist)object).setDescription(this.getElementString());
			}
			else if(name.equals("picture") && object instanceof Playlist){
				((Playlist)object).setPicture(this.getElementString());
			}
			else if(name.equals("add")){
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					if(name.equals("i")){
						this.reader.getElementText(); /* Skip. */
					}
					else if(name.equals("items")){
						StringTokenizer tokenizer = new StringTokenizer(this.getElementString(), ",\n");
						
						if(object instanceof Playlist){
							List<Track> tracks = new ArrayList<Track>();
							
							while(tokenizer.hasMoreTokens()){
								tracks.add(new Track(tokenizer.nextToken().substring(0, 32)));
							}
							
							((Playlist)object).setTracks(tracks);
						}
						else if(object instanceof PlaylistContainer){
							List<Playlist> playlists = new ArrayList<Playlist>();
							
							while(tokenizer.hasMoreTokens()){
								playlists.add(new Playlist(tokenizer.nextToken().substring(0, 32)));
							}
							
							((PlaylistContainer)object).setPlaylists(playlists);
						}
						else{
							throw new XMLParserException(
								"Unexpected object '" + object + "'", this.reader.getLocation()
							);
						}
					}
					else{
						throw new XMLParserException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
			}
			else if(name.equals("set-attribute")){
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					if(name.equals("i")){
						this.reader.getElementText(); /* Skip. */
					}
					else if(name.equals("key")){
						this.reader.getElementText(); /* Skip. */
					}
					else if(name.equals("value")){
						this.reader.getElementText(); /* Skip. */
					}
					else{
						throw new XMLParserException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
	}
	
	private PlaylistConfirmation parsePlaylistConfirmation() throws XMLStreamException, XMLParserException {
		PlaylistConfirmation confirmation = new PlaylistConfirmation();
		String               name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("rid")){
				this.getElementString(); /* Skip. */
			}
			else if(name.equals("version")){
				String[] parts = this.getElementString().split(",");
				
				confirmation.setRevision(Long.parseLong(parts[0]));
				confirmation.setChecksum(Long.parseLong(parts[2]));
				confirmation.setCollaborative(Integer.parseInt(parts[3]) == 1);
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return confirmation;
	}
	
	/**
	 * Parse {@code xml} into an object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return An object if successful, null if not.
	 */
	public static Object parse(byte[] data, String encoding, String id){
		try{
			XMLPlaylistParser parser = new XMLPlaylistParser(new ByteArrayInputStream(data), encoding);
			
			return parser.parse(id);
		}
		catch(XMLStreamException e){
			return null;
		}
		catch(XMLParserException e){
			return null;
		}
	}
	
	/**
	 * Parse {@code xml} into a {@link PlaylistContainer} object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link PlaylistContainer} object if successful, null if not.
	 */
	public static PlaylistContainer parsePlaylistContainer(byte[] data, String encoding){
		/* Wrap xml data in corrent document element. */
		Object playlistContainer = parse(
			("<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlists>" +
				new String(data) +
			"</playlists>").getBytes(),
			encoding, null
		);
		
		if(playlistContainer instanceof PlaylistContainer){
			return (PlaylistContainer)playlistContainer;
		}
		
		return null;
	}
	
	/**
	 * Parse {@code xml} into a {@link Playlist} object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link Playlist} object if successful, null if not.
	 */
	public static Playlist parsePlaylist(byte[] data, String encoding, String id){
		/* Wrap xml data in corrent document element. */
		Object playlist = parse(
			("<?xml version=\"1.0\" encoding=\"utf-8\" ?><playlist>" +
				new String(data) +
			"</playlist>").getBytes(),
			encoding, id
		);
		
		if(playlist instanceof Playlist){
			return (Playlist)playlist;
		}
		
		return null;
	}
	
	/**
	 * Parse {@code xml} into a {@link PlaylistConfirmation} object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link PlaylistConfirmation} object if successful, null if not.
	 */
	public static PlaylistConfirmation parseConfirmation(byte[] data, String encoding){
		/* Wrap xml data in corrent document element. */
		Object playlist = parse(
			("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + new String(data)).getBytes(),
			encoding, null
		);
		
		if(playlist instanceof PlaylistConfirmation){
			return (PlaylistConfirmation)playlist;
		}
		
		return null;
	}
}
