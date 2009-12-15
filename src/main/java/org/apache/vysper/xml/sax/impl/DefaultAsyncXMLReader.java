/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xml.sax.impl;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.util.Stack;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.xml.decoder.DecodingException;
import org.apache.vysper.xml.decoder.ParticleDecoder;
import org.apache.vysper.xml.decoder.XMLParticle;
import org.apache.vysper.xml.sax.AsyncXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultAsyncXMLReader implements AsyncXMLReader {

	private ErrorHandler errorHandler = new DefaultHandler();
	private ContentHandler contentHandler = new DefaultHandler();
	
	private boolean documentStarted = false;
	private boolean parserClosed = false;
	
	// element names as {uri}qname
	private Stack<String> elements = new Stack<String>();
	
	/**
	 * {@inheritDoc}
	 */
    public boolean getFeature (String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
    	return false;
    }


	/**
	 * {@inheritDoc}
	 */
    public void setFeature (String name, boolean value)
		throws SAXNotRecognizedException, SAXNotSupportedException {
    	
    }


	/**
	 * {@inheritDoc}
	 */
    public Object getProperty (String name)
	throws SAXNotRecognizedException, SAXNotSupportedException {
    	return null;
    }

	/**
	 * {@inheritDoc}
	 */
    public void setProperty (String name, Object value)
    	throws SAXNotRecognizedException, SAXNotSupportedException {
    	
    }

	/**
	 * {@inheritDoc}
	 */
    public void setEntityResolver (EntityResolver resolver) {
    	throw new RuntimeException("Entity resolver not supported");
    }

	/**
	 * {@inheritDoc}
	 */
    public EntityResolver getEntityResolver () {
    	throw new RuntimeException("Entity resolver not supported");
    }

	/**
	 * {@inheritDoc}
	 */
    public void setDTDHandler (DTDHandler handler) {
    	throw new RuntimeException("DTD handler not supported");
    }

    public DTDHandler getDTDHandler () {
    	throw new RuntimeException("DTD handler not supported");
    }

	/**
	 * {@inheritDoc}
	 */
    public void setContentHandler (ContentHandler handler) {
    	this.contentHandler = handler;
    }

	/**
	 * {@inheritDoc}
	 */
    public ContentHandler getContentHandler () {
    	return contentHandler;
    }

	/**
	 * {@inheritDoc}
	 */
    public void setErrorHandler (ErrorHandler handler) {
    	this.errorHandler = handler;
    }

    /**
	 * {@inheritDoc}
	 */
    public ErrorHandler getErrorHandler () {
    	return errorHandler;
    }

    private String toFQEN(XMLParticle particle) throws DecodingException {
    	return "{}" + particle.getElementName();
    }
    
    private void fatalError(String msg) throws SAXException {
    	parserClosed = true;
    	errorHandler.fatalError(new SAXParseException(msg, null));
    }
    
	/**
	 * {@inheritDoc}
	 */
    public void parse (ByteBuffer buffer, CharsetDecoder decoder) throws IOException, SAXException {
    	if(parserClosed) {
    		throw new SAXException("Parser closed");
    	}
    	
    	try {
			XMLParticle particle = ParticleDecoder.decodeParticle(buffer, decoder);
			while(particle != null) {
				if(!documentStarted) {
					// TODO handle exception
					contentHandler.startDocument();
					documentStarted = true;
				}
				
				if(particle.isOpeningElement()) {
					// TODO handle exception
					contentHandler.startElement("", particle.getElementName(), particle.getElementName(), new DefaultAttributes());
					elements.push(toFQEN(particle));
				}

				if(particle.isClosingElement()) {
					String fqen = elements.pop();
					if(!fqen.equals(toFQEN(particle))) {
						fatalError("Incorrect closing element");
						return;
					}

					// TODO handle exception
					contentHandler.endElement("", particle.getElementName(), particle.getElementName());
				}

				if(particle.isText()) {
					if(elements.size() == 0) {
						fatalError("Illegal placement of text");
						return;
					}
					
					
					char[] ch = particle.getContent().toCharArray();
					// TODO handle exception
					contentHandler.characters(ch, 0, ch.length);
				}
				
				if(elements.size() == 0) {
					parserClosed = true;
					// TODO handle exception
					contentHandler.endDocument();
				}
				
				particle = ParticleDecoder.decodeParticle(buffer, decoder);
			}
		} catch (IOException e) {
			throw e;
		} catch(Exception e) {
			fatalError(e.getMessage());
		}
    }



}