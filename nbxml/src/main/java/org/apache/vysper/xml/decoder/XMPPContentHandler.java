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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SAX content handler for the purpose of parsing an incoming XMPP XML stream.
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

    private XMLElementListener listener;

    public XMLElementListener getListener() {
        return listener;
    }

    public void setListener(XMLElementListener listener) {
        this.listener = listener;
    }

    public XMPPContentHandler() {
    }

    public XMPPContentHandler(XMLElementBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // TODO handle start and length
        if (builder != null) {
            builder.addText(new String(ch));
        }

    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        depth--;
        if (depth == 1) {
            // complete stanza, emit
            emit();
        } else if (depth == 0) {
            // End of stream. Handled in endDocument().
        } else {
            builder.endInnerElement();
        }

    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        // increase element depth
        depth++;
        if (builder == null) {
            builder = builderFactory.createBuilder(localName, uri, extractPrefix(qName), null, null);
        } else {
            builder.startInnerElement(localName, uri);
        }

        for (int i = 0; i < atts.getLength(); i++) {
            builder.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getValue(i));
        }

        if (depth == 1) {
            // outer stream:stream element, needs to be dispatched right away
            emit();
        }
    }

    private void emit() {
        XMLElement element = builder.build();

        if (log.isDebugEnabled()) {
            log.debug("Decoder writing stanza: {}", new Renderer(element).getComplete());
        }

        if (listener != null) {
            listener.element(element);
        }

        builder = null;
    }

    private String extractPrefix(String qname) {
        int index = qname.indexOf(':');
        if (index > -1) {
            return qname.substring(0, index);
        } else {
            return "";
        }
    }

    /**
     * End of the XMPP stream. Tell the XML listener we're done.
     */
    public void endDocument() throws SAXException { 
    	this.listener.close();
    }

    /**
     * {@inheritDoc}
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void endPrefixMapping(String prefix) throws SAXException { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void processingInstruction(String target, String data) throws SAXException { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentLocator(Locator locator) { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void skippedEntity(String name) throws SAXException { /* ignore */
    }

    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        depth = 0;
        builder = null;
    }
}
