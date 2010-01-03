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
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultNonBlockingXMLReader implements NonBlockingXMLReader {

	private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
	private static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
	
	private ErrorHandler errorHandler = new DefaultHandler();
	private ContentHandler contentHandler = new DefaultHandler();

	private XMLParser parser;
	
	private Map<String, Boolean> features = new HashMap<String, Boolean>();
	
	public DefaultNonBlockingXMLReader() {
		// set default features
		features.put(FEATURE_NAMESPACES, true);
		features.put(FEATURE_NAMESPACE_PREFIXES, false);
	}
	
	/**
	 * {@inheritDoc}
	 */
    public boolean getFeature (String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
    	if(features.containsKey(name)) {
    		return features.get(name);
    	} else {
    		throw new SAXNotRecognizedException("Unknown feature");
    	}
    }


	/**
	 * {@inheritDoc}
	 */
    public void setFeature (String name, boolean value)
		throws SAXNotRecognizedException, SAXNotSupportedException {
    	
    	// features must be set before parsing starts
    	if(parser != null) {
    		throw new SAXNotSupportedException("Feature can not be set during parsing");
    	}
    	
    	if(features.containsKey(name)) {
    		// TODO make configurable fatures and values easier to manage
    		if(name.equals(FEATURE_NAMESPACES) && value) {
    			// ok
    		} else if(name.equals(FEATURE_NAMESPACE_PREFIXES) && !value) {
    			// ok
    		} else {
    			throw new SAXNotSupportedException("Not supported");
    		}
    	} else {
    		throw new SAXNotRecognizedException("Unknown feature");
    	}
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

	/**
	 * {@inheritDoc}
	 */
    public void parse (ByteBuffer buffer, CharsetDecoder decoder) throws IOException, SAXException {
    	if(parser == null) {
    		parser = new XMLParser(contentHandler, errorHandler);
    	}
    	
		parser.parse(buffer, decoder);
    }



}