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
package org.apache.vysper.xml.decoder;

import org.apache.vysper.xml.fragment.AbstractXMLElementBuilder;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.impl.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


/**
 * partitions the incoming byte stream in particles of XML. either those enclosed by '<' and '>', or the text inbetween.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPContentHandler implements ContentHandler {

	private Logger log = LoggerFactory.getLogger(XMPPContentHandler.class);
	
	private XMLElementBuilderFactory builderFactory = new XMLElementBuilderFactory();
	
	// TODO change into StanzaBuilder when moved into core
	@SuppressWarnings("unchecked")
	private AbstractXMLElementBuilder builder;
	private int depth = 0;
	
	private StanzaListener listener;
	
	public StanzaListener getListener() {
		return listener;
	}


	public void setListener(StanzaListener listener) {
		this.listener = listener;
	}

	public static interface StanzaListener {
		void stanza(XMLElement element);
	}
	
    public XMPPContentHandler() {
    }
	
	
    public XMPPContentHandler(XMLElementBuilderFactory builderFactory) {
    	this.builderFactory = builderFactory;
    }

	
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO handle start and length
		builder.addText(new String(ch));
		
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		depth--;
		if(depth == 1) {
			// complete stanza, emit
			emitStanza();
		} else if(depth == 0) {
			// end stanza:stanza element
			// TODO handle
		} else {
			builder.endInnerElement();
		}
		
	}

	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// increase element depth
		depth++;
		
		if(builder == null) {
			builder = builderFactory.createBuilder(localName, uri, extractPrefix(qName), null, null);
		} else {
			builder.startInnerElement(localName, uri);
		}
		
		for(int i = 0; i<atts.getLength(); i++) {
			builder.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getValue(i));
		}
		
		if(depth == 1) {
			// outer stanza:stanza element, needs to be dispatched right away
			emitStanza();
		}
	}
	
	private void emitStanza() {
		XMLElement element = builder.build();
		
		if(log.isDebugEnabled()) {
			log.debug("Decoder writing stanza: {}", new Renderer(element).getComplete());
		}
		
		if(listener != null) {
			listener.stanza(element);
		}
		
		builder = null;
	}
	
	private String extractPrefix(String qname) {
		int index = qname.indexOf(':'); 
		if(index > -1) {
			return qname.substring(0, index);
		} else {
			return "";
		}
	}

	public void endDocument() throws SAXException { /* ignore */ }
	
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException { /* ignore */ }

	public void endPrefixMapping(String prefix) throws SAXException { /* ignore */ }

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException { /* ignore */ }

	public void processingInstruction(String target, String data)
			throws SAXException { /* ignore */ }

	public void setDocumentLocator(Locator locator) { /* ignore */ }

	public void skippedEntity(String name) throws SAXException { /* ignore */ }

	public void startDocument() throws SAXException { /* ignore */ }


	
}
