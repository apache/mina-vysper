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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLText;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.cyberneko.pull.XMLEvent;
import org.cyberneko.pull.XMLPullParser;
import org.cyberneko.pull.event.CharactersEvent;
import org.cyberneko.pull.event.DocumentEvent;
import org.cyberneko.pull.event.ElementEvent;
import org.cyberneko.pull.event.PrefixMappingEvent;
import org.cyberneko.pull.parsers.Xerces2;

/**
 * uses Nekopull/Xerves2 to parse XMPP XML streams. implementations differ only in the way they aquire
 * the XMLInputSource (stream, reader, file, string).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractNekopullStreamParser implements StreamParser {

    final protected static String DEFAULT_OPENING_XML_ELEMENT_NAME = "defaultOpener";

    /**
     * to allow XML fragments to be parsed, which contain XML like this:
     * <element1/><element2/>
     * which is not allowed for top level XML (only one top level xml element, not more)
     * an artificial opening element is prepended so that following elements are not top level anymore
     * TODO check if this workaround can be removed by relaxing xerces strictness
     */
    final protected static String DEFAULT_OPENING_XML_ELEMENT = "<" + DEFAULT_OPENING_XML_ELEMENT_NAME + ">";


    private XMLPullParser parser = null;
    private Map<String, String> prefixesToNamespace = new HashMap<String, String>();

    protected abstract XMLInputSource getInputSource();

    /**
     * initializes the parser and checks if a document is present
     * @throws ParsingException
     */
    private void open() throws ParsingException {
        if (parser != null) throw new RuntimeException("cannot reopen stream");
        XMLPullParser parserTemp = createParser();
        try {
            parserTemp.setInputSource(getInputSource());
        } catch (IOException e) {
            throw new ParsingException("failed to bind XML input source");
        }
        parser = parserTemp;

        XMLEvent event = getNextXMLEvent();
        if (event.type != XMLEvent.DOCUMENT) throw new ParsingException("XML document event expected, but was type = " + event.type);
    }

    protected Xerces2 createParser() {
        return new Xerces2();
    }

    /**
     * blocking operation until next stanza is ready
     * @return
     * @throws ParsingException
     */
    public Stanza getNextStanza() throws ParsingException {
        if (parser == null) open();

        XMLEvent event = null;
        while(true) {
            event = getNextXMLEvent();
            if (event == null) return null; // end of stream
            if (event.type == XMLEvent.DOCUMENT && !((DocumentEvent) event).start) return null; // end of stream

            if (event.type == XMLEvent.PREFIX_MAPPING) {
                // namespace directive found
                PrefixMappingEvent prefixMappingEvent = (PrefixMappingEvent) event;
                String prefix = prefixMappingEvent.prefix;
                if (prefixesToNamespace.containsKey(prefix)) {
                    throw new IllegalStateException("nested namespaces not supported by currently parser. xmlns=" + prefix);
                }
                prefixesToNamespace.put(prefix, prefixMappingEvent.uri);
            }

            if (event.type == XMLEvent.ELEMENT) break;

            // ignore all other XML (RFC3920#11.1) and continue
            // TODO add logging of ignored xml event
        }

        ElementEvent startElementEvent = (ElementEvent) event;
        if (!startElementEvent.start) throw new ParsingException("XML start element expected, but was end element " + startElementEvent.element.rawname);
        String namespaceURI = resolveNamespace(startElementEvent);
        String name = startElementEvent.element.localpart;

        if (DEFAULT_OPENING_XML_ELEMENT_NAME.equals(name)) return getNextStanza(); // skip artificial xml element

        List<Attribute> stanzaAttributes = fillAttributesFromXML(startElementEvent);

        List<XMLFragment> xmlFragments = new ArrayList<XMLFragment>();

        if (!"stream".equals(name)) {
            if (!fillDeep(startElementEvent.element.rawname, xmlFragments)) throw new ParsingException("XML end element not found as expected");
        }

        return new Stanza(name, null, stanzaAttributes, xmlFragments);
    }

    private boolean fillDeep(String name, List<XMLFragment> xmlFragments) throws ParsingException {
        XMLEvent event;
        while (true) {
            event = getNextXMLEvent();
            if (event == null) return false;

            if (event.type == XMLEvent.ELEMENT) {
                ElementEvent elementEvent = (ElementEvent) event;
                if (elementEvent.start) {
                    // collect element basics
                    String namespaceURI = resolveNamespace(elementEvent);
                    String innerName = elementEvent.element.localpart;

                    // collect detail data
                    List<Attribute> stanzaAttributes = fillAttributesFromXML(elementEvent);
                    List<XMLFragment> xmlInnerFragments = new ArrayList<XMLFragment>();
                    fillDeep(elementEvent.element.rawname, xmlInnerFragments); // (return value can be savely ignored)

                    // create element with all collected data
                    XMLElement xmlInnerElement = new XMLElement(innerName, namespaceURI, stanzaAttributes, xmlInnerFragments);
                    xmlFragments.add(xmlInnerElement);
                } else {
                    return name.equals(elementEvent.element.rawname); // succeed if exact end element found and all is balanced
                }
            } else if (event.type == XMLEvent.CDATA) {
                xmlFragments.add(new XMLText(event.toString()));
            } else if (event.type == XMLEvent.CHARACTERS) {
                XMLString xmlString = ((CharactersEvent) event).text;
                xmlFragments.add(new XMLText(xmlString.toString()));
            } else {
                // ignore other types, as of XMPP spec
            }
        }
    }

    private String resolveNamespace(ElementEvent elementEvent) {
        String prefix = elementEvent.element.prefix;

        String namespaceURI = prefixesToNamespace.get(prefix);

        return namespaceURI;
    }

    private List<Attribute> fillAttributesFromXML(ElementEvent elementEvent) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        XMLAttributes xmlAttributes = elementEvent.attributes;
        for (int i = 0; i < xmlAttributes.getLength(); i++) {
            String qName = xmlAttributes.getQName(i);
            String value = xmlAttributes.getValue(i);
            attributes.add(new Attribute(qName, value));
        }
        return attributes;
    }

    private XMLEvent getNextXMLEvent() throws ParsingException {
        XMLEvent event = null;
        try {
            event = parser.nextEvent();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ParsingException("internal xerces error", e);
        } catch (IOException e) {
            throw new ParsingException("could not step to next XML element", e);
        } catch (XMLParseException e) {
            if (e.getMessage().contains("XML document structures must start and end within the same entity")) return null; // end of file
            //throw new ParsingException("could not step to next XML element", e);
        }
        return event;
    }
}
