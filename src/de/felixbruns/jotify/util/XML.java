package de.felixbruns.jotify.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XML {
	public static XMLElement load(Reader xml){
		/* Document and elements */
		DocumentBuilder documentBuilder = null;
		Document        document        = null;
		
		/* Create document. */
		try{
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document        = documentBuilder.parse(new InputSource(xml));
		}
		catch(Exception e){
			return null;
		}
		
		/* Return root element. */
		return new XMLElement(document.getDocumentElement());
	}
	
	public static XMLElement load(File xml) throws FileNotFoundException{
		return load(new FileReader(xml));
	}
	
	public static XMLElement load(String xml){
		return load(new StringReader(xml));
	}
	
	public static XMLElement load(byte[] xml, Charset charset){
		return load(new String(xml, charset));
	}
}
