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

package org.apache.vysper.xmpp.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.DocumentContentHandler;
import org.apache.vysper.xml.decoder.XMLElementListener;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.xml.sax.SAXException;

import static java.util.Optional.ofNullable;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLParserUtil {

    /**
     * Parses a complete XML document. If the XML string contains errors, a
     * {@link SAXException} will be thrown. If the XML string is not a complete
     * XML document, null will be returned
     * @param xml A string with a complete XML document
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public static XMLElement parseDocument(String xml) throws IOException, SAXException {
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();

        DocumentContentHandler contentHandler = new DocumentContentHandler();
        reader.setContentHandler(contentHandler);

        final List<XMLElement> documents = new ArrayList<XMLElement>();
        
        contentHandler.setListener(new XMLElementListener() {
            public void element(XMLElement element) {
                documents.add(element);
            }
            public void close() {}
        });

        IoBuffer buffer = IoBuffer.wrap(xml.getBytes("UTF-8"));
        reader.parse(buffer, CharsetUtil.getDecoder());

        if(!documents.isEmpty()) {
            return documents.get(0);
        } else {
            return null;
        }
    }

    public static XMLElement parseRequiredDocument(String xml) throws IOException, SAXException {
        return ofNullable(parseDocument(xml)).orElseThrow(() -> new IllegalStateException("Parsed element should not be null"));
    }
    
}
