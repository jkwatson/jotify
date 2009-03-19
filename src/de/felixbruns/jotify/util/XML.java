package de.felixbruns.jotify.util;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XML {
	public static XMLElement load(String xml){
		/* Document and elements */
		DocumentBuilder documentBuilder = null;
		Document        document        = null;
		
		/* Create document. */
		try{
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document        = documentBuilder.parse(new InputSource(new StringReader(xml)));
		}
		catch(Exception e){
			return null;
		}
		
		/* Return root element. */
		return new XMLElement(document.getDocumentElement());
	}
	
	public static XMLElement load(byte[] xml){
		return load(new String(xml));
	}
}
