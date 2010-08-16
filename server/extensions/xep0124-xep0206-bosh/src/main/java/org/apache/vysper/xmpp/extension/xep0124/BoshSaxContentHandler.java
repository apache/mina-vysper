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
package org.apache.vysper.xmpp.extension.xep0124;

import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.mina.codec.StanzaBuilderFactory;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.fragment.AbstractXMLElementBuilder;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SAX handler that constructs BOSH requests by parsing the XML from BOSH clients.
 * <p>
 * This class is similar to {@link XMPPContentHandler}
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshSaxContentHandler implements ContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoshSaxContentHandler.class);

    private final BoshHandler boshHandler;

    private final HttpServletRequest request;

    private final StanzaBuilderFactory builderFactory;

    private AbstractXMLElementBuilder<StanzaBuilder, Stanza> builder;

    private int depth = 0;

    private boolean isBodyPayloadDecoded = false;

    public BoshSaxContentHandler(BoshHandler boshHandler, HttpServletRequest req) {
        this.boshHandler = boshHandler;
        request = req;
        builderFactory = new StanzaBuilderFactory();
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        // TODO handle start and length
        if (builder != null) {
            builder.addText(new String(ch));
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        depth--;
        if (depth == 0 && !isBodyPayloadDecoded) {
            // complete body, emit
            emitStanza();
        } else {
            builder.endInnerElement();
        }
    }

    private void emitStanza() {
        isBodyPayloadDecoded = true;
        XMLElement element = builder.build();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("BOSH decoding request: {}", new Renderer(element).getComplete());
        }
        boshHandler.process(request, (Stanza) element);
        builder = null;
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        depth++;
        if (builder == null) {
            builder = builderFactory.createBuilder(localName, uri, extractPrefix(qName), null, null);
        } else {
            builder.startInnerElement(localName, uri);
        }

        for (int i = 0; i < atts.getLength(); i++) {
            builder.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getValue(i));
        }
    }

    private String extractPrefix(String qname) {
        int index = qname.indexOf(':');
        if (index > -1) {
            return qname.substring(0, index);
        } else {
            return "";
        }
    }

    public void setDocumentLocator(Locator locator) {
        // ignore
    }

    public void startDocument() throws SAXException {
        // ignore
    }

    public void endDocument() throws SAXException {
        // ignore
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // ignore
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        // ignore
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        // ignore
    }

    public void processingInstruction(String target, String data) throws SAXException {
        // ignore
    }

    public void skippedEntity(String name) throws SAXException {
        // ignore
    }

}
