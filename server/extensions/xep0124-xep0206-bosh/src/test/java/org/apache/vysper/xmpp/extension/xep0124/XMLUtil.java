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

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.mina.codec.StanzaBuilderFactory;
import org.apache.vysper.xml.decoder.XMLElementBuilderFactory;
import org.apache.vysper.xml.fragment.AbstractXMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class XMLUtil implements ContentHandler {

    private final XMLElementBuilderFactory builderFactory;

    private final IoBuffer input;

    private final NonBlockingXMLReader reader;

    @SuppressWarnings("rawtypes")
    private AbstractXMLElementBuilder builder;

    private int depth = 0;

    private boolean isBodyPayloadDecoded = false;

    private Stanza retStanza;

    public XMLUtil(String xml) {
        input = IoBuffer.allocate(xml.length());
        input.setAutoExpand(true);
        input.put(xml.getBytes());
        input.flip();
        builderFactory = new StanzaBuilderFactory();
        reader = new DefaultNonBlockingXMLReader();
        reader.setContentHandler(this);
    }
    
    public XMLUtil(InputStream xml) throws IOException {
        input = IoBuffer.allocate(1024);
        input.setAutoExpand(true);
        byte[] buf = new byte[1024];
        for (;;) {
            int n = xml.read(buf);
            if (n == -1) {
                break;
            }
            input.put(buf, 0, n);
        }
        input.flip();
        builderFactory = new StanzaBuilderFactory();
        reader = new DefaultNonBlockingXMLReader();
        reader.setContentHandler(this);
    }

    public Stanza parse() throws IOException, SAXException {
        reader.parse(input, CharsetUtil.getDecoder());
        return retStanza;
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
        retStanza = (Stanza) element;
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
