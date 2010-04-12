/*
 * Copyright 2008, James Murty (www.jamesmurty.com)
 * Copyright 2010, Felix Bruns <felixbruns@web.de> (Modifed for use in jotify)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * The original code is available from the Google Code repository at:
 * http://code.google.com/p/java-xmlbuilder
 */
package de.felixbruns.jotify.util;

import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML Builder is a utility that creates simple XML documents using relatively 
 * sparse Java code. It is intended to allow for quick and painless creation of 
 * XML documents where you might otherwise be tempted to use concatenated 
 * strings, rather than face the tedium and verbosity of coding with 
 * JAXP (http://jaxp.dev.java.net/).
 * <p>
 * Internally, XML Builder uses JAXP to build a standard W3C 
 * {@link Document} model (DOM) that you can easily export as a 
 * string, or access and manipulate further if you have special requirements.
 * </p> 
 * <p>
 * The XMLBuilder class serves as a wrapper of {@link Element} nodes,
 * and provides a number of utility methods that make it simple to
 * manipulate the underlying element and the document to which it belongs. 
 * In essence, this class performs dual roles: it represents a specific XML 
 * node, and also allows manipulation of the entire underlying XML document.
 * The platform's default {@link DocumentBuilderFactory} and 
 * {@link DocumentBuilder} classes are used to build the document.
 * </p>
 *  
 * @author James Murty
 * @author Felix Bruns
 */
public class XMLBuilder {
	/**
	 * A {@link Document} that stores the underlying XML document operated on by
	 * {@link XMLBuilder} instances. This document object belongs to the root node
	 * of a document, and is shared by this node with all other XMLBuilder instances.
	 */
	private Document document;
	
	/**
	 * The underlying element represented by this builder node.
	 */
	private Element element;
	
	/**
	 * Construct a new {@link XMLBuilder} that wraps the given {@link Document}.
	 * 
	 * @param document A {@link Document} that the {@link XMLBuilder}
	 * will manage and manipulate.
	 */
	protected XMLBuilder(Document document){
		this.document = document;
		this.element  = document.getDocumentElement();
	}
	
	/**
	 * Construct a new {@link XMLBuilder} that wraps the given {@link Element}.
	 * 
	 * @param element The {@link Element} that this {@link XMLBuilder} will wrap.
	 * This element may be part of the XML document, or it may be a new element
	 * that is to be added to the document.
	 */
	protected XMLBuilder(Element element){
		this.document = element.getOwnerDocument();
		this.element  = element;
	}
	
	/**
	 * Construct a new {@link XMLBuilder}. The underlying document will be created
	 * with the given root element, and the builder returned by this method
	 * will serve as the starting-point for any further document additions.
	 * 
	 * @param name The name of the documents root element.
	 * 
	 * @return An {@link XMLBuilder} that can be used to add more nodes to
	 * the XML document.
	 * 
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 */
	public static XMLBuilder create(String name){
		try{
			DocumentBuilder builder  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document        document = builder.newDocument();
			
			document.appendChild(document.createElement(name));
			
			return new XMLBuilder(document);
		}
		catch(ParserConfigurationException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Construct a new {@link XMLBuilder}. The underlying document will be created
	 * and the builder returned by this method will serve as the starting-point for
	 * any further document additions.
	 * 
	 * @return An {@link XMLBuilder} that can be used to add more nodes to
	 * the XML document.
	 * 
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 */
	public static XMLBuilder create(){
		return create("xml-builder");
	}
	
	private static String toUTF8(String s){
		return new String(s.getBytes(Charset.forName("UTF-8")));
	}
	
	/**
	 * Returns the {@link XMLBuilder} representing the root element of the XML document.
	 * 
	 * @return An {@link XMLBuilder} instance.
	 */
	public XMLBuilder root(){
		return new XMLBuilder(this.document);
	}
	
	/**
	 * Add a child node with text content to the document, and return the
	 * {@link XMLBuilder} representing the new child.
	 * 
	 * @param name  The name of the new child node.
	 * @param value The text content of the new child node or {@code null}.
	 * 
	 * @return An {@link XMLBuilder} representing the new child.
	 * 
	 * @throws IllegalStateException
	 * If you attempt to add a child element to an XML node that already 
	 * contains a text node value.
	 */
	public XMLBuilder element(String name, String value){
		NodeList children = this.element.getChildNodes();
		Element  element  = this.document.createElement(name);
			
		for(int i = 0; i < children.getLength(); i++){
			if(Element.TEXT_NODE == children.item(i).getNodeType()){
				throw new IllegalStateException(
					"Can't add subelement <" + name + "> to element " +
					"<" + this.element.getNodeName() + "> that already " +
					"contains the text node: " + children.item(i)
				);
			}
		}
		
		if(value != null){
			element.appendChild(this.document.createTextNode(XMLBuilder.toUTF8(value)));
		}
		
		this.element.appendChild(element);
		
		return new XMLBuilder(element);
	}
	
	public XMLBuilder element(String name, String format, Object... args){
		return this.element(name, String.format(format, args));
	}
	
	public XMLBuilder element(String name){
		return this.element(name, null);
	}
	
	/**
	 * Synonym for {@link #element(String)}.
	 * 
	 * @param name  The name of the new child node.
	 * @param value The text content of the new child node or {@code null}.
	 * 
	 * @return An {@link XMLBuilder} representing the new child.
	 * 
	 * @throws IllegalStateException
	 * If you attempt to add a child element to an XML node that already 
	 * contains a text node value.
	 */
	public XMLBuilder e(String name, String value){
		return this.element(name, value);
	}
	
	public XMLBuilder e(String name, String format, Object... args){
		return this.element(name, String.format(format, args));
	}
	
	public XMLBuilder e(String name){
		return this.element(name);
	}
	
	/**
	 * Add an attribute to the element represented by this {@link XMLBuilder}
	 * and return the {@link XMLBuilder} representing the element to which the 
	 * attribute was added (<strong>not</strong> the new attribute node).
	 * 
	 * @param name  The attribute's name.
	 * @param value The attribute's value.
	 * 
	 * @return The {@link XMLBuilder} representing the {@link Element} to which
	 * the attribute was added.
	 */
	public XMLBuilder attribute(String name, String value){
		this.element.setAttribute(name, XMLBuilder.toUTF8(value));
		
		return this;
	}
	
	/**
	 * Synonym for {@link #attribute(String, String)}.
	 * 
	 * @param name  The attribute's name.
	 * @param value The attribute's value.
	 * 
	 * @return The {@link XMLBuilder} representing the {@link Element} to which
	 * the attribute was added.
	 */
	public XMLBuilder a(String name, String value){
		return this.attribute(name, value);
	}
	
	/**
	 * Add a text value to the element represented by this {@link XMLBuilder}
	 * and return the {@link XMLBuilder} representing the element to which the
	 * text was added (<strong>not</strong> the new text node).
	 * 
	 * @param value The text value to add to the element.
	 * 
	 * @return The {@link XMLBuilder} representing the element to which the text was added.
	 */
	public XMLBuilder text(String value){
		this.element.appendChild(this.document.createTextNode(XMLBuilder.toUTF8(value)));
		
		return this;
	}
	
	/**
	 * Synonm for {@link #text(String)}.
	 * 
	 * @param value The text value to add to the element.
	 * 
	 * @return The {@link XMLBuilder} representing the element to which the text was added.
	 */
	public XMLBuilder t(String value){
		return this.text(value);
	}
	
	/**
	 * Add a formatted string to the element represented by this {@link XMLBuilder}
	 * and return the {@link XMLBuilder} representing the element to which the
	 * text was added (<strong>not</strong> the new text node).
	 * 
	 * @param format The format string.
	 * @param args   The format arguments.
	 * 
	 * @return The {@link XMLBuilder} representing the element to which the numeric value was added.
	 */
	public XMLBuilder format(String format, Object... args){
		return this.text(String.format(format, args));
	}
	
	/**
	 * Synonm for {@link #format(String, Object...)}.
	 * 
	 * @param format The format string.
	 * @param args   The format arguments.
	 * 
	 * @return The {@link XMLBuilder} representing the element to which the numeric value was added.
	 */
	public XMLBuilder f(String format, Object... args){
		return this.format(format, args);
	}
	
	/**
	 * Return the {@link XMLBuilder} representing the n<em>th</em> ancestor element 
	 * of this node, or the root node if n exceeds the documents depth.
	 *   
	 * @param steps The number of parent elements to step over while navigating up
	 * the chain of node ancestors. A steps value of 1 will find a nodes parent, 2
	 * will find its grandparent etc.
	 * 
	 * @return The {@link XMLBuilder} representing the n<em>th</em> ancestor of this
	 * node, or the root node if this is reached before the n<em>th</em> parent is found.
	 */
	public XMLBuilder up(int steps){
		Node node = (Node)this.element;
		
		while(node.getParentNode() != null && steps-- > 0){
			node = node.getParentNode();
		}
		
		return new XMLBuilder((Element)node);
	}
	
	/**
	 * Return the {@link XMLBuilder} representing the parent of the current node.
	 * 
	 * @return The {@link XMLBuilder} representing the parent of this node, or the
	 * root node if this method is called on the root node. 
	 */
	public XMLBuilder up(){
		return this.up(1);
	}
	
	/**
	 * Serialize the XML document to a string. If the initial {@link XMLBuilder} was
	 * created using {@link #create(String)}, a normal XML document is returned. If
	 * it was created using {@link #create()}, the XML declaration is ommited and it
	 * also doesn't have a document element.
	 * 
	 * @return The XML document as a string
	 * 
	 * @throws TransformerException
	 */
	public String toString(){
		try{
			StringWriter  writer	 = new StringWriter();
			StreamResult  result     = new StreamResult(writer);
			Transformer   serializer = TransformerFactory.newInstance().newTransformer();
			Element       element    = this.document.getDocumentElement();
			
			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
			if(element.getNodeName().equals("xml-builder")){
				NodeList nodeList = element.getChildNodes();
				
				serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				
				for(int i = 0; i < nodeList.getLength(); i++){
					serializer.transform(new DOMSource(nodeList.item(i)), result);
				}
			}
			else{
				serializer.transform(new DOMSource(element), result);
			}
			
			return writer.toString();
		}
		catch(TransformerException e){
			throw new RuntimeException(e);
		}
	}
}
