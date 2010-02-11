package de.felixbruns.jotify.media.parser;

import java.io.InputStream;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class XMLParser {
	protected XMLStreamReader reader;
	
	/**
	 * Create a new stream parser from the given input stream.
	 * 
	 * @param stream An {@link InputStream} stream to parse.
	 */
	protected XMLParser(InputStream stream, String encoding) throws XMLStreamException {
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
	 * Get an attributes value.
	 * 
	 * @param attribute An attribute name.
	 * 
	 * @return The value of the given attribute.
	 */
	protected String getAttributeString(String attribute){
		return this.reader.getAttributeValue(null, attribute);
	}
	
	/**
	 * Get the current elements contents as string.
	 * 
	 * @return A string.
	 * 
	 * @throws XMLStreamException
	 */
	protected String getElementString() throws XMLStreamException {
		return this.reader.getElementText().trim();
	}
	
	/**
	 * Get the current elements contents an integer.
	 * 
	 * @return An integer.
	 * 
	 * @throws XMLStreamException
	 */
	protected int getElementInteger() throws XMLStreamException {
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
	protected float getElementFloat() throws XMLStreamException {
		try{
			return Float.parseFloat(this.reader.getElementText());
		}
		catch(NumberFormatException e){
			return Float.NaN;
		}
	}
	
	/**
	 * Get the current elements contents an integer.
	 * 
	 * @return An integer.
	 * 
	 * @throws XMLStreamException
	 */
	protected boolean getElementBoolean() throws XMLStreamException {
		return Boolean.parseBoolean(this.reader.getElementText());
	}
}
