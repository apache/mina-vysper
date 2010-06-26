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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TestHandler implements ContentHandler, ErrorHandler {

    public static interface TestEvent {

    }

    public static class StartDocumentEvent implements TestEvent {

    }

    public static class EndDocumentEvent implements TestEvent {

    }

    public static class StartElementEvent implements TestEvent {
        private String uri;

        private String localName;

        private String qName;

        private Attributes atts;

        public StartElementEvent(String uri, String localName, String qName, Attributes atts) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
            this.atts = atts;
        }

        public String getURI() {
            return uri;
        }

        public String getLocalName() {
            return localName;
        }

        public String getQName() {
            return qName;
        }

        public Attributes getAtts() {
            return atts;
        }
    }

    public static class EndElementEvent implements TestEvent {
        private String uri;

        private String localName;

        private String qName;

        public EndElementEvent(String uri, String localName, String qName) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
        }

        public String getURI() {
            return uri;
        }

        public String getLocalName() {
            return localName;
        }

        public String getQName() {
            return qName;
        }
    }

    public static class CharacterEvent implements TestEvent {
        private String characters;

        public CharacterEvent(char[] ch, int start, int length) {
            char[] trimmed = new char[length];
            System.arraycopy(ch, start, trimmed, 0, length);
            this.characters = new String(trimmed);
        }

        public String getCharacters() {
            return characters;
        }
    }

    public static class FatalErrorEvent implements TestEvent {
        private Exception exception;

        public FatalErrorEvent(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }

    public static class ErrorEvent implements TestEvent {
        private Exception exception;

        public ErrorEvent(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }

    public static class WarningEvent implements TestEvent {
        private Exception exception;

        public WarningEvent(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }

    private List<TestEvent> events = new ArrayList<TestEvent>();

    public List<TestEvent> getEvents() {
        return events;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        System.out.println("sax characters: " + new String(ch));
        events.add(new CharacterEvent(ch, start, length));

    }

    public void endDocument() throws SAXException {
        System.out.println("sax end document");
        events.add(new EndDocumentEvent());
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        System.out.println("sax end element: " + qName);
        events.add(new EndElementEvent(uri, localName, qName));

    }

    public void endPrefixMapping(String prefix) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void processingInstruction(String target, String data) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void setDocumentLocator(Locator locator) {
        // TODO Auto-generated method stub

    }

    public void skippedEntity(String name) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void startDocument() throws SAXException {
        System.out.println("sax start document");
        events.add(new StartDocumentEvent());
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        System.out.println("sax start element " + qName);
        events.add(new StartElementEvent(uri, localName, qName, atts));

    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void error(SAXParseException exception) throws SAXException {
        events.add(new ErrorEvent(exception));
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        events.add(new FatalErrorEvent(exception));
    }

    public void warning(SAXParseException exception) throws SAXException {
        events.add(new WarningEvent(exception));
    }

}