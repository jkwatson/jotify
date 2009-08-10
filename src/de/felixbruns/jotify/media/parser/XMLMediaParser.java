package de.felixbruns.jotify.media.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Biography;
import de.felixbruns.jotify.media.Disc;
import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Image;
import de.felixbruns.jotify.media.Restriction;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;

public class XMLMediaParser implements XMLStreamConstants {
	private XMLStreamReader reader;
	
	private static final int SUPPORTED_RESULT_VERSION = 1;
	private static final int SUPPORTED_ARTIST_VERSION = 1;
	private static final int SUPPORTED_ALBUM_VERSION  = 1;
	
	/**
	 * Create a new stream parser from the given input stream.
	 * 
	 * @param stream An {@link InputStream} stream to parse.
	 */
	private XMLMediaParser(InputStream stream, String encoding) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		
		this.reader = factory.createXMLStreamReader(stream, encoding);
		this.reader = factory.createFilteredReader(this.reader, new StreamFilter(){
			public boolean accept(XMLStreamReader reader){
				return reader.isStartElement() || reader.isEndElement() ||
					   (reader.isCharacters() && !reader.isWhiteSpace());
			}
		});
	}
	
	/**
	 * Parse the input stream as one of {@link Result}, {@link Artist},
	 * {@link Album} or {@link Track}, depending on the document element.
	 * 
	 * @return An {@link Object} which can then be cast.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Object parse() throws XMLStreamException, XMLMediaParseException {
		String name;
		
		/* Check if reader is currently on a start element. */
		if(this.reader.getEventType() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Check current element name and start parsing it. */
			if(name.equals("result")){
				return this.parseResult();
			}
			else if(name.equals("toplist")){
				return this.parseResult();
			}
			else if(name.equals("artist")){
				return this.parseArtist();
			}
			else if(name.equals("album")){
				return this.parseAlbum();
			}
			else if(name.equals("track")){
				return this.parseTrack();
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		throw new IllegalStateException("Reader is not on a start element!");
	}
	
	/**
	 * Parse the input stream as a {@link Result}.
	 * 
	 * @return A {@link Result} object.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Result parseResult() throws XMLStreamException, XMLMediaParseException {
		Result result = new Result();
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("version")){
				int version = this.getElementInteger();
				
				/* Check version. */
				if(version > SUPPORTED_RESULT_VERSION){
					throw new XMLMediaParseException(
						"Unsupported <result> version " + version, this.reader.getLocation()
					);
				}
			}
			else if(name.equals("did-you-mean")){
				result.setSuggestion(this.getElementString());
			}
			else if(name.equals("total-artists")){
				result.setTotalArtists(this.getElementInteger());
			}
			else if(name.equals("total-albums")){
				result.setTotalAlbums(this.getElementInteger());
			}
			else if(name.equals("total-tracks")){
				result.setTotalTracks(this.getElementInteger());
			}
			else if(name.equals("artists")){
				result.setArtists(parseArtists());
			}
			else if(name.equals("albums")){
				result.setAlbums(parseAlbums());
			}
			else if(name.equals("tracks")){
				result.setTracks(parseTracks());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return result;
	}
	
	/**
	 * Parse the input stream as a list of artists.
	 * 
	 * @return A {@link List} of {@link Artist} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<Artist> parseArtists() throws XMLStreamException, XMLMediaParseException {
		List<Artist> artists = new ArrayList<Artist>();
		String       name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("artist")){
				artists.add(parseArtist());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return artists;
	}
	
	/**
	 * Parse the input stream as a list of albums.
	 * 
	 * @return A {@link List} of {@link Album} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<Album> parseAlbums() throws XMLStreamException, XMLMediaParseException {
		List<Album> albums = new ArrayList<Album>();
		String      name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("album")){
				albums.add(parseAlbum());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return albums;
	}
	
	/**
	 * Parse the input stream as a list of tracks.
	 * 
	 * @return A {@link List} of {@link Track} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<Track> parseTracks() throws XMLStreamException, XMLMediaParseException {
		List<Track> tracks = new ArrayList<Track>();
		String      name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("track")){
				tracks.add(parseTrack());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return tracks;
	}
	
	/**
	 * Parse the input stream as an {@link Artist}.
	 * 
	 * @return An {@link Artist} object.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Artist parseArtist() throws XMLStreamException, XMLMediaParseException {
		Artist artist = new Artist();
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("version")){
				int version = this.getElementInteger();
				
				/* Check version. */
				if(version > SUPPORTED_ARTIST_VERSION){
					throw new XMLMediaParseException(
						"Unsupported <album> version " + version, this.reader.getLocation()
					);
				}
			}
			else if(name.equals("id")){
				artist.setId(this.getElementString());
			}
			else if(name.equals("name")){
				artist.setName(this.getElementString());
			}
			else if(name.equals("portrait")){
				artist.setPortrait(parseImage());
			}
			else if(name.equals("genres")){
				String[] genres = this.getElementString().split(",");
				
				artist.setGenres(Arrays.asList(genres));
			}
			else if(name.equals("years-active")){
				String[] years = this.getElementString().split(",");
				
				artist.setYearsActive(Arrays.asList(years));
			}
			else if(name.equals("popularity")){
				artist.setPopularity(this.getElementFloat());
			}
			else if(name.equals("bios")){
				artist.setBios(parseBios());
			}
			else if(name.equals("similar-artists")){
				artist.setSimilarArtists(parseArtists());
			}
			else if(name.equals("albums")){
				artist.setAlbums(parseAlbums());
			}
			else if(name.equals("restrictions")){
				artist.setRestrictions(parseRestrictions());
			}
			else if(name.equals("external-ids")){
				artist.setExternalIds(parseExternalIds());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return artist;
	}
	
	/**
	 * Parse the input stream as an {@link Album}.
	 * 
	 * @return An {@link Album} object.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Album parseAlbum() throws XMLStreamException, XMLMediaParseException {
		Album  album = new Album();
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("version")){
				int version = this.getElementInteger();
				
				/* Check version. */
				if(version > SUPPORTED_ALBUM_VERSION){
					throw new XMLMediaParseException(
						"Unsupported <album> version " + version, this.reader.getLocation()
					);
				}
			}
			else if(name.equals("id")){
				album.setId(this.getElementString());
			}
			else if(name.equals("name")){
				album.setName(this.getElementString());
			}
			else if(name.equals("artist") || name.equals("artist-name")){
				Artist artist = (album.getArtist() != null)?album.getArtist():new Artist();
				
				artist.setName(this.getElementString());
				
				album.setArtist(artist);
			}
			else if(name.equals("artist-id")){
				Artist artist = (album.getArtist() != null)?album.getArtist():new Artist();
				
				artist.setId(this.getElementString());
				
				album.setArtist(artist);
			}
			else if(name.equals("album-type")){
				album.setType(this.getElementString());
			}
			else if(name.equals("cover")){
				String cover = this.getElementString();
				
				/* Check if string is empty to prevent exception in setCover(). */
				if(!cover.isEmpty()){
					album.setCover(cover);
				}
			}
			else if(name.equals("popularity")){
				album.setPopularity(this.getElementFloat());
			}
			else if(name.equals("review")){
				album.setReview(this.getElementString());
			}
			else if(name.equals("year")){
				album.setYear(this.getElementInteger());
			}
			/* TODO: currently skipped. */
			else if(name.equals("copyright")){
				/* Go to next element and check if it is a start element. */
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					/* Process depending on element name. */
					if(name.equals("c")){
						/* Skip text. */
						this.getElementString();
					}
					else if(name.equals("p")){
						/* Skip text. */
						this.getElementString();
					}
					else{
						throw new XMLMediaParseException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
			}
			/* TODO: currently skipped. */
			else if(name.equals("links")){
				skipLinks();
			}
			else if(name.equals("restrictions")){
				album.setRestrictions(parseRestrictions());
			}
			/* Seems to be deprecated. */
			else if(name.equals("allowed")){
				/* Skip text. */
				this.getElementString();
			}
			/* Seems to be deprecated. */
			else if(name.equals("forbidden")){
				/* Skip text. */
				this.getElementString();
			}
			else if(name.equals("discs")){
				List<Disc> discs = new ArrayList<Disc>();
				
				/* Go to next element and check if it is a start element. */
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					/* Process depending on element name. */
					if(name.equals("disc")){
						List<Track> tracks = new ArrayList<Track>();
						Disc        disc   = new Disc();
						
						/* Go to next element and check if it is a start element. */
						while(this.reader.next() == START_ELEMENT){
							name = this.reader.getLocalName();
							
							if(name.equals("disc-number")){
								disc.setNumber(this.getElementInteger());
							}
							else if(name.equals("name")){
								disc.setName(this.getElementString());
							}
							else if(name.equals("track")){
								tracks.add(parseTrack());
							}
							else{
								throw new XMLMediaParseException(
									"Unexpected element '<" + name + ">'", this.reader.getLocation()
								);
							}
						}
						
						/* Set disc tracks. */
						disc.setTracks(tracks);
						discs.add(disc);
					}
					else{
						throw new XMLMediaParseException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
				
				album.setDiscs(discs);
			}
			else if(name.equals("similar-albums")){
				List<Album> similarAlbums = new ArrayList<Album>();
				
				/* Go to next element and check if it is a start element. */
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					/* Process depending on element name. */
					if(name.equals("id")){
						similarAlbums.add(new Album(this.getElementString()));
					}
					else{
						throw new XMLMediaParseException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
				
				/* Set similar albums. */
				album.setSimilarAlbums(similarAlbums);
			}
			else if(name.equals("external-ids")){
				album.setExternalIds(parseExternalIds());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return album;
	}
	
	/**
	 * Parse the input stream as a {@link Track}.
	 * 
	 * @return A {@link Track} object.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Track parseTrack() throws XMLStreamException, XMLMediaParseException {
		Track  track = new Track();
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("id")){
				track.setId(this.getElementString());
			}
			/* TODO: currently skipped. */
			else if(name.equals("redirect")){
				/* Skip text. */
				this.getElementString();
			}
			else if(name.equals("title")){
				track.setTitle(this.getElementString());
			}
			else if(name.equals("artist")){
				Artist artist = (track.getArtist() != null)?track.getArtist():new Artist();
				
				artist.setName(this.getElementString());
				
				track.setArtist(artist);
			}
			else if(name.equals("artist-id")){
				Artist artist = (track.getArtist() != null)?track.getArtist():new Artist();
				
				artist.setId(this.getElementString());
				
				track.setArtist(artist);
			}
			else if(name.equals("album")){
				Album album = (track.getAlbum() != null)?track.getAlbum():new Album();
				
				album.setName(this.getElementString());
				
				track.setAlbum(album);
			}
			else if(name.equals("album-id")){
				Album album = (track.getAlbum() != null)?track.getAlbum():new Album();
				
				album.setId(this.getElementString());
				
				track.setAlbum(album);
			}
			else if(name.equals("album-artist")){
				Album  album  = (track.getAlbum() != null)?track.getAlbum():new Album();
				Artist artist = (album.getArtist() != null)?album.getArtist():new Artist();
				
				artist.setName(this.getElementString());
				
				album.setArtist(artist);
				track.setAlbum(album);
			}
			else if(name.equals("album-artist-id")){
				Album  album  = (track.getAlbum() != null)?track.getAlbum():new Album();
				Artist artist = (album.getArtist() != null)?album.getArtist():new Artist();
				
				artist.setId(this.getElementString());
				
				album.setArtist(artist);
				track.setAlbum(album);
			}
			else if(name.equals("year")){
				track.setYear(this.getElementInteger());
			}
			else if(name.equals("track-number")){
				track.setTrackNumber(this.getElementInteger());
			}
			else if(name.equals("length")){
				int length = this.getElementInteger();
				
				if(length > 0){
					track.setLength(length);
				}
			}
			else if(name.equals("files")){
				track.setFiles(parseFiles());
			}
			/* TODO: currently skipped. */
			else if(name.equals("links")){
				skipLinks();
			}
			/* TODO: currently skipped. */
			else if(name.equals("album-links")){
				skipLinks();
			}
			else if(name.equals("cover")){
				track.setCover(this.getElementString());
			}
			else if(name.equals("popularity")){
				track.setPopularity(this.getElementFloat());
			}
			else if(name.equals("restrictions")){
				track.setRestrictions(parseRestrictions());
			}
			/* Seems to be deprecated. */
			else if(name.equals("allowed")){
				/* Skip text. */
				this.getElementString();
			}
			/* Seems to be deprecated. */
			else if(name.equals("forbidden")){
				/* Skip text. */
				this.getElementString();
			}
			else if(name.equals("similar-tracks")){
				List<Track> similarTracks = new ArrayList<Track>();
				
				/* Go to next element and check if it is a start element. */
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					/* Process depending on element name. */
					if(name.equals("id")){
						similarTracks.add(new Track(this.getElementString()));
					}
					else{
						throw new XMLMediaParseException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
				
				/* Set similar tracks. */
				track.setSimilarTracks(similarTracks);
			}
			else if(name.equals("external-ids")){
				track.setExternalIds(parseExternalIds());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		/* If album artist of this track is not yet set, then set it. */
		if(track.getAlbum() != null && track.getAlbum().getArtist() == null){
			track.getAlbum().setArtist(track.getArtist());
		}
		
		return track;
	}
	
	/**
	 * Parse the input stream as an image.
	 * 
	 * @return An {@link Image} object.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Image parseImage() throws XMLStreamException, XMLMediaParseException {
		Image  image = new Image();
		String name;
		int    type;
		
		/* Go to next element and check if it is a start element. */
		while((type = this.reader.next()) == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("id")){
				image.setId(this.getElementString());
			}
			else if(name.equals("width")){
				image.setWidth(this.getElementInteger());
			}
			else if(name.equals("height")){
				image.setHeight(this.getElementInteger());
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		/* If the reader is not at an end element, it is at some character event. */
		if(type != END_ELEMENT){
			/* Read image id from element text (special case). */
			image.setId(this.reader.getText());
			
			/* Skip to end element. */
			this.reader.next();
		}
		
		return image;
	}
	
	/**
	 * Parse the input stream as a {@link List} of biographies.
	 * 
	 * @return A {@link List} of {@link Biography} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<Biography> parseBios() throws XMLStreamException, XMLMediaParseException {
		List<Biography> bios = new ArrayList<Biography>();
		String    name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("bio")){
				Biography bio = new Biography();
				
				/* Go to next element and check if it is a start element. */
				while(this.reader.next() == START_ELEMENT){
					name = this.reader.getLocalName();
					
					/* Process depending on element name. */
					if(name.equals("text")){
						bio.setText(this.getElementString());
					}
					else if(name.equals("portraits")){
						List<Image> portraits = new ArrayList<Image>();
						
						/* Go to next element and check if it is a start element. */
						while(this.reader.next() == START_ELEMENT){
							name = this.reader.getLocalName();
							
							/* Process depending on element name. */
							if(name.equals("portrait")){
								portraits.add(parseImage());
							}
							else{
								throw new XMLMediaParseException(
									"Unexpected element '<" + name + ">'", this.reader.getLocation()
								);
							}
						}
						
						/* Add portraits to biography. */
						bio.setPortraits(portraits);
					}
					else{
						throw new XMLMediaParseException(
							"Unexpected element '<" + name + ">'", this.reader.getLocation()
						);
					}
				}
				
				/* Add biograhpy to list. */
				bios.add(bio);
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return bios;
	}
	
	/**
	 * Parse the input stream as a list of files.
	 * 
	 * @return A {@link List} of {@link File} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<File> parseFiles() throws XMLStreamException, XMLMediaParseException {
		List<File> files = new ArrayList<File>();
		String     name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("file")){
				files.add(new File(
					getAttributeString("id"), getAttributeString("format")
				));
				
				/* Skip to end element, since we only read the attributes. */
				this.reader.next();
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return files;
	}
	
	/**
	 * Parse the input stream as a list of restrictions.
	 * 
	 * @return A {@link List} of {@link Restriction} objects.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private List<Restriction> parseRestrictions() throws XMLStreamException, XMLMediaParseException {
		List<Restriction> restrictions = new ArrayList<Restriction>();
		String            name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("restriction")){
				restrictions.add(new Restriction(
					getAttributeString("allowed"),
					getAttributeString("forbidden"),
					getAttributeString("catalogues")
				));
				
				/* Skip to end element since we only read the attributes. */
				this.reader.next();
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return restrictions;
	}
	
	/**
	 * Parse the input stream as a map of external ids.
	 * 
	 * @return A {@link Map} containing external ids.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private Map<String, String> parseExternalIds() throws XMLStreamException, XMLMediaParseException {
		Map<String, String> externalIds = new HashMap<String, String>();
		String              name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("external-id")){
				externalIds.put(
					getAttributeString("type"), getAttributeString("id")
				);
				
				/* Skip to end element since we only read the attributes. */
				this.reader.next();
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return externalIds;
	}
	
	/**
	 * Skip any {@literal <link>} elements.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLMediaParseException
	 */
	private void skipLinks() throws XMLStreamException, XMLMediaParseException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Process depending on element name. */
			if(name.equals("link")){
				/* Skip text. */
				this.getElementString();
			}
			else{
				throw new XMLMediaParseException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
	}
	
	/**
	 * Get an attributes value.
	 * 
	 * @param attribute An attribute name.
	 * 
	 * @return The value of the given attribute.
	 */
	private String getAttributeString(String attribute){
		return this.reader.getAttributeValue(null, attribute);
	}
	
	/**
	 * Get the current elements contents as string.
	 * 
	 * @return A string.
	 * 
	 * @throws XMLStreamException
	 */
	private String getElementString() throws XMLStreamException {
		return this.reader.getElementText();
	}
	
	/**
	 * Get the current elements contents an integer.
	 * 
	 * @return An integer.
	 * 
	 * @throws XMLStreamException
	 */
	private int getElementInteger() throws XMLStreamException {
		try{
			return Integer.parseInt(this.reader.getElementText());
		}
		catch(NumberFormatException e){
			return 0;
		}
	}
	
	/**
	 * Get the current elements contents a floating-point number.
	 * 
	 * @return A float.
	 * 
	 * @throws XMLStreamException
	 */
	private float getElementFloat() throws XMLStreamException {
		try{
			return Float.parseFloat(this.reader.getElementText());
		}
		catch(NumberFormatException e){
			return Float.NaN;
		}
	}
	
	/**
	 * Parse {@code xml} into an object using the specified {@code encoding}.
	 * 
	 * @param xml      The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return An object if successful, null if not.
	 */
	public static Object parse(byte[] xml, String encoding){
		try{
			XMLMediaParser parser = new XMLMediaParser(new ByteArrayInputStream(xml), encoding);
			
			return parser.parse();
		}
		catch(XMLStreamException e){
			return null;
		}
		catch(XMLMediaParseException e){
			return null;
		}
	}
	
	/**
	 * Parse {@code xml} into a {@link Result} object using the specified {@code encoding}.
	 * 
	 * @param xml      The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link Result} object if successful, null if not.
	 */
	public static Result parseResult(byte[] xml, String encoding){
		Object result = parse(xml, encoding);
		
		if(result instanceof Result){
			return (Result)result;
		}
		
		return null;
	}
	
	/**
	 * Parse {@code xml} into an {@link Artist} object using the specified {@code encoding}.
	 * 
	 * @param xml      The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return An {@link Artist} object if successful, null if not.
	 */
	public static Artist parseArtist(byte[] xml, String encoding){
		Object artist = parse(xml, encoding);
		
		if(artist instanceof Artist){
			return (Artist)artist;
		}
		
		return null;
	}
	
	/**
	 * Parse {@code xml} into a {@link Album} object using the specified {@code encoding}.
	 * 
	 * @param xml      The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link Album} object if successful, null if not.
	 */
	public static Album parseAlbum(byte[] xml, String encoding){
		Object album = parse(xml, encoding);
		
		if(album instanceof Album){
			return (Album)album;
		}
		
		return null;
	}
}
