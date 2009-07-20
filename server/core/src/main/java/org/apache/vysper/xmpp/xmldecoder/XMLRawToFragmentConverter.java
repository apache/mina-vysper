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
package org.apache.vysper.xmpp.xmldecoder;

import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLText;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * determines, if a sequence of XMLParticles is balanced, and if it is, converts them to XMLFragments (which are understood
 * by core XMPP engine.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLRawToFragmentConverter {
    private static final String CHAR_ATTR_EQUALS = "=";
    private static final String CHAR_ATTR_SINGLEQUOTE = "'";
    private static final String CHAR_ATTR_DOUBLEQUOTE = "\"";

    /**
     * tracks, where we are when parsing start elements and their attributes (which are composed of key/value pairs)
     */
    enum AttributeParseState {
        BEFORE_KEY, IN_KEY, BEFORE_EQUALS, BEFORE_VALUE, IN_VALUE
    }

    public boolean isBalanced(List<XMLParticle> particles) throws DecodingException {

        // stack, where opened elements are pushed and closing are pulled
        Stack<XMLParticle> openedElements = new Stack<XMLParticle>();

        for (XMLParticle particle : particles) {
            if (particle.isSpecialElement()) {
                continue; // special elements do not have nested elements
            } else if (particle.isOpeningOnlyElement()) {
                openedElements.push(particle);
            } else if (particle.isText()) {
                continue; // text does not have nested elements
            } else if (particle.isOpeningElement()) {
                // is also closing (because opening-only are already handled),
                // so don't push them
                continue;
            } else if (particle.isClosingElement()) {
                // TODO handle </stream:stream> here as a special case
                if (openedElements.isEmpty()) return false; // not balanced, because no matching opener on stack
                if (!openedElements.peek().getElementName().equals(particle.getElementName())) {
                    throw new IllegalStateException();
                }
                openedElements.pop();
            } else throw new IllegalStateException();
        }

        boolean startOfStream = (openedElements.size() == 1 && openedElements.get(0).getElementName().equals("stream:stream"));
        return openedElements.isEmpty() || startOfStream;
    }

    public XMLFragment convert(List<XMLParticle> particles) throws DecodingException {
        List<XMLFragment> xmlFragmentList = convert(particles, null);
        if (xmlFragmentList.size() != 1) throw new DecodingException("converter only allows for one top xml element, all others must be inner elements");
        return xmlFragmentList.get(0);
    }

    protected List<XMLFragment> convert(List<XMLParticle> particles, String stopElementName) throws DecodingException {
        boolean createStanza = stopElementName == null;

        List<XMLFragment> fragments = new ArrayList<XMLFragment>();

        while (!particles.isEmpty()) {
            XMLParticle particle = particles.remove(0);

            if (particle.isOpeningElement() && particle.getContent().startsWith("<!--")) {
                throw new UnsupportedXMLException("XML comments are unsupported in XMPP");
            }

            if (particle.isOpeningElement() && particle.isClosingElement()) {
                // has no inner elements, no need to find matching closer
                XMLElement completeElement = getElementForOpening(particle, createStanza);
                if (completeElement != null) fragments.add(completeElement);
            } else if (particle.isOpeningOnlyElement()) {
                XMLElement incompleteElement = getElementForOpening(particle, createStanza);
                List<XMLFragment> innerFragments = convert(particles, particle.getElementName());
                XMLElement completedElement = createElementOrStanza(incompleteElement.getName(), incompleteElement.getAttributes(), incompleteElement.getNamespacePrefix(), innerFragments, createStanza);
                fragments.add(completedElement);
            } else if (particle.isClosingOnlyElement()) {
                String closingElementName = getElementNameForClosingOnly(particle);
                boolean properlyClosedTopLevel = (stopElementName == null && particles.isEmpty());
                boolean properlyClosedNested = closingElementName.equals(stopElementName);
                if (properlyClosedTopLevel || properlyClosedNested) return fragments;
                else throw new DecodingException("closing xml elements mismatch. expected '" + stopElementName + "' but found '"  + closingElementName + "'");
            } else {
                fragments.add(new XMLText(particle.getContent()));
            }

        }
        if (stopElementName != null) {
            // of the remaining elements, all must be texts
            for (XMLParticle particle : particles) {
                if (!particle.isText()) throw new IllegalStateException("missing closing xml elements");
                else fragments.add(new XMLText(particle.getContent()));
            }
        }
        return fragments;
    }

    private String getElementNameForClosingOnly(XMLParticle particle) throws DecodingException {
        if (!particle.isClosingOnlyElement()) throw new IllegalArgumentException();

        String elementText = particle.getContent();
        String coreText = elementText.substring(2, elementText.length() - 1);
        if (coreText.startsWith(" ")) throw new DecodingException("closing element name must follow immediately after '</' in " + coreText);
        return coreText.trim();
    }

    private XMLElement getElementForOpening(XMLParticle particle, boolean createStanza) throws DecodingException {
        if (!particle.isOpeningElement()) throw new IllegalArgumentException();

        String elementName = particle.getElementName();
        if (elementName == null) throw new DecodingException("element name could not be determined for " + particle.getContent());

        String content = particle.getContentWithoutElement();
        List<Attribute> attributes = parseAttributes(content);
        elementName = elementName.trim();
        return createElementOrStanza(elementName, attributes, null, (List<XMLFragment>) null, createStanza);
    }

    private XMLElement createElementOrStanza(String elementName, List<Attribute> attributes, String namespacePrefix, List<XMLFragment> innerFragments, boolean createStanza) throws DecodingException {
        int i = elementName.indexOf(":");
        if (i >= 1) {
            namespacePrefix = elementName.substring(0, i);
            elementName = elementName.substring(i + 1);
            if ("".equals(namespacePrefix) || "".equals(elementName)) throw new DecodingException("illegal element name " + namespacePrefix + ":" + elementName);
        } else if (i == 0) {
            // element something like "<:foo> this is legal XML. in this case, acccording to XML spec section 2.3,
            // the colon belongs to the element name and is not a separator for the namespace.
            // but we do not support that.
            throw new DecodingException("unsupported legal XML: colon at start of element name and no namespace specified");
        }
        if (createStanza) return new Stanza(elementName, namespacePrefix, attributes, innerFragments);
        else return new XMLElement(elementName, namespacePrefix, attributes, innerFragments);
    }

    private List<Attribute> parseAttributes(String content) throws DecodingException {

        List<Attribute> attributes = new ArrayList<Attribute>();

        AttributeParseState currentState = AttributeParseState.BEFORE_KEY;

        char[] chars = content.toCharArray();

        Stack<Character> stack = new Stack<Character>();
        for (int i = chars.length - 1; i >= 0; i--) {
            char aChar = chars[i];
            if (aChar == '<') throw new DecodingException("illegal opening brace inside xml element: " + content);
            stack.push(aChar);
        }

        String key = null;
        String value = null;
        StringBuilder collect = null;
        Character valueDelimiter = null;
        while(!stack.isEmpty()) {
            Character character = stack.peek();

            if (character == null) {
                if (currentState == AttributeParseState.BEFORE_KEY) return attributes;
                else return null; // we are in the middle of something
            }

            switch (currentState) {
                case BEFORE_EQUALS:
                    if (Character.isWhitespace(character)) stack.pop();
                    else if (character == '=') {
                        currentState = AttributeParseState.BEFORE_VALUE;
                        stack.pop();
                    } else throw new DecodingException("'=' expected after attribute name " + key);
                    break;
                case BEFORE_KEY:
                    if (Character.isWhitespace(character)) stack.pop();
                    else if (character == '/' || character == '!' || character == '?') {
                        character = stack.pop();
                        if (stack.isEmpty()) {
                            throw new DecodingException("preliminary end of element");
                        }
                        character = stack.pop();
                        if (character != '>' ) throw new DecodingException("preliminary end of element");
                        return attributes;
                    } else if (character == '>' ) {
                        character = stack.pop();
                        if (!stack.isEmpty()) throw new DecodingException("preliminary closing of tag");
                        return attributes;
                    } else if (Character.isLetterOrDigit(character)) {
                        currentState = AttributeParseState.IN_KEY;
                        collect = new StringBuilder();
                        collect.append(character);
                        stack.pop();
                    }
                    break;
                case BEFORE_VALUE:
                    if (Character.isWhitespace(character)) stack.pop();
                    else if (character == '\'' || character == '\"') {
                        currentState = AttributeParseState.IN_VALUE;
                        stack.pop();
                        valueDelimiter = character;
                        collect = new StringBuilder();
                    }
                    break;
                case IN_KEY:
                    if (!Character.isWhitespace(character) && character != '=') {
                        stack.pop();
                        collect.append(character);
                    } else {
                        currentState = AttributeParseState.BEFORE_EQUALS;
                        key = collect.toString();
                        collect = null;
                    }
                    break;
                case IN_VALUE:
                    if (character != valueDelimiter) {
                        stack.pop();
                        collect.append(character);
                    } else {
                        stack.pop();
                        currentState = AttributeParseState.BEFORE_KEY;
                        value = collect.toString();
                        collect = null;
                        attributes.add(new Attribute(key, value));
                    }
                    break;
            }
        }

        return attributes;
    }
}
