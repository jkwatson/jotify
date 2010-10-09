package de.felixbruns.jotify.media.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import de.felixbruns.jotify.media.User;

public class XMLUserParser extends XMLParser implements XMLStreamConstants {
	/**
	 * Create a new stream parser from the given input stream.
	 * 
	 * @param stream An {@link InputStream} stream to parse.
	 */
	private XMLUserParser(InputStream stream, String encoding) throws XMLStreamException {
		super(stream, encoding);
	}
	
	/**
	 * Parse the input stream as a {@link User} object.
	 * 
	 * @return An {@link Object} which can then be cast.
	 * 
	 * @throws XMLStreamException
	 * @throws XMLParserException
	 */
	private User parse(User user) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Check if reader is currently on a start element. */
		if(this.reader.getEventType() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			/* Check current element name and start parsing it. */
			if(name.equals("products")){
				return this.parseUser(user);
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		throw new IllegalStateException("Reader is not on a start element!");
	}
	
	private User parseUser(User user) throws XMLStreamException, XMLParserException {
		String name;
		
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			name = this.reader.getLocalName();
			
			if(name.equals("product")){
				this.parseProduct(user);
			}
			else if(name.equals("token")){
				this.getElementString(); /* Skip. */
			}
			else{
				throw new XMLParserException(
					"Unexpected element '<" + name + ">'", this.reader.getLocation()
				);
			}
		}
		
		return user;
	}
	
	private void parseProduct(User user) throws XMLStreamException, XMLParserException {
		/* Go to next element and check if it is a start element. */
		while(this.reader.next() == START_ELEMENT){
			user.setProperty(this.reader.getLocalName(), this.getElementString());
		}
	}
	
	/**
	 * Parse {@code xml} into an object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return An object if successful, null if not.
	 */
	public static User parse(byte[] data, String encoding, User user){
		try{
			XMLUserParser parser = new XMLUserParser(new ByteArrayInputStream(data), encoding);
			
			return parser.parse(user);
		}
		catch(XMLStreamException e){
			return null;
		}
		catch(XMLParserException e){
			return null;
		}
	}
	
	/**
	 * Parse {@code xml} into a {@link User} object using the specified {@code encoding}.
	 * 
	 * @param data     The xml as bytes.
	 * @param encoding The encoding to use.
	 * 
	 * @return A {@link User} object if successful, null if not.
	 */
	public static User parseUser(byte[] data, String encoding, User user){
		return parse(data, encoding, user);
	}
}
