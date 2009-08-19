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

package org.apache.vysper.xmpp.stanza;

import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.Renderer;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLText;
import org.apache.vysper.xmpp.xmlfragment.NamespaceAttribute;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaBuilder {

    public static StanzaBuilder createIQStanza(Entity from, Entity to, IQStanzaType type, String id) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq");
        if (from != null) stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        if (to != null) stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        stanzaBuilder.addAttribute("type", type.value());
        stanzaBuilder.addAttribute("id", id);
        return stanzaBuilder;
    }

    public static StanzaBuilder createMessageStanza(Entity from, Entity to, String lang, String body) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("message");
        stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        if(lang != null) stanzaBuilder.addAttribute("xml:lang", lang);
        stanzaBuilder.startInnerElement("body").addText(body).endInnerElement();
        return stanzaBuilder;
    }

    public static StanzaBuilder createMessageStanza(Entity from, Entity to, MessageStanzaType type, String lang, String body) {
        StanzaBuilder stanzaBuilder = createMessageStanza(from, to, lang, body);
        if (type != null) stanzaBuilder.addAttribute("type", type.value());
        return stanzaBuilder;
    }

    public static StanzaBuilder createPresenceStanza(Entity from, Entity to, String lang, PresenceStanzaType type, String show, String status) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("presence");
        if (from != null) stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        if (to != null) stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        if (lang != null) stanzaBuilder.addAttribute("xml:lang", lang);
        if (type != null) stanzaBuilder.addAttribute("type", type.value());
        if (show != null) {
            stanzaBuilder.startInnerElement("show").addText(show).endInnerElement();
        }
        if (status != null) {
            stanzaBuilder.startInnerElement("status").addText(status).endInnerElement();
        }
        return stanzaBuilder;
    }

    public static StanzaBuilder createDirectReply(XMPPCoreStanza original, boolean fromIsServerOnly, String type) {
        if (original == null) throw new IllegalArgumentException();

        StanzaBuilder stanzaBuilder = new StanzaBuilder(original.getName(), original.getNamespaceURI(), original.getNamespacePrefix());
        // reverse to and from
        Entity newTo = original.getFrom();
        if (newTo != null) {
            stanzaBuilder.addAttribute("to", newTo.getFullQualifiedName());
        }
        Entity newFrom = original.getTo();
        if (newFrom != null) {
            if (fromIsServerOnly) newFrom = new EntityImpl(null, newFrom.getDomain(), null);
            stanzaBuilder.addAttribute("from", newFrom.getFullQualifiedName());
        }
        stanzaBuilder.addAttribute("type", type);
        if (original.getID() != null) stanzaBuilder.addAttribute("id", original.getID());

        return stanzaBuilder;
    }

    /**
     * creates a clone of the given original stanza into the returned StanzaBuilder, but replaces all attributes in the
     * top-level original element with the values from the given attribute list.
     * this way, the builder can be coerced into the final stanza (calling StanzaBuilder#getFinalStanza() or be used to
     * add additional attributes and inner elements.
     * @param original
     * @param replacingAttributes - if this is a short list, iteration is more efficient than hash mapping
     * @param deep
     * @return
     */
    public static StanzaBuilder createClone(XMLElement original, boolean deep, List<Attribute> replacingAttributes) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder(original.getName(), original.getNamespacePrefix());

        List<Attribute> replacingAttributesCopy = new ArrayList<Attribute>(replacingAttributes);

        List<Attribute> originalAttributes = original.getAttributes();
        for (Attribute originalAttribute : originalAttributes) {
            boolean wasReplaced = false;
            for (Iterator<Attribute> it = replacingAttributesCopy.iterator(); it.hasNext();) {
                Attribute replacingAttribute = it.next();
                if (replacingAttribute == null) continue;
                if (replacingAttribute.getName().equals(originalAttribute.getName())) {
                    stanzaBuilder.addAttribute(replacingAttribute);
                    it.remove(); // this has been processed
                    wasReplaced = true;
                    break;
                }
            }
            if (!wasReplaced) stanzaBuilder.addAttribute(originalAttribute);
        }

        // add remaining replacements, which are actually additions
        for (Attribute additionalAttribute : replacingAttributesCopy) {
            stanzaBuilder.addAttribute(additionalAttribute);
        }

        // copy over immutable inner elements
        if (deep && original.getInnerElements() != null) {
            List<XMLElement> innerElements = original.getInnerElements();
            for (XMLElement innerElement : innerElements) {
                stanzaBuilder.addPreparedElement(innerElement);
            }
        }

        return stanzaBuilder;
    }


    class ElementStruct {
        public ElementStruct parentElement = null;
        public XMLElement element = null;
        public List<Attribute> attributes = null;
        public List<XMLFragment> innerFragments = null;
    }

    /**
     * parent hierarchy for current element
     */
    private Stack<ElementStruct> stack = new Stack<ElementStruct>();
    private ElementStruct currentElement = null;
    private Stanza resultingStanza = null;
    private boolean isReset = false;


    public StanzaBuilder(String stanzaName) {
        this(stanzaName, null);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI) {
        this(stanzaName, namespaceURI, null);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI, String namespacePrefix) {
        startNewElement(stanzaName, namespaceURI, namespacePrefix, true);
        resultingStanza = (Stanza)currentElement.element;
        stack.push(currentElement);
    }
    
    private void startNewElement(String name, String namespaceURI, String namespacePrefix, boolean isStanza) {
        // TODO assert that name does not contain namespace (":")
        // TODO handle the namespace, given by URI, currently always NULL in Stanza/XMLElement constructors
        ElementStruct element = new ElementStruct();
        element.attributes = new ArrayList<Attribute>();
        element.innerFragments = new ArrayList<XMLFragment>();
        if (isStanza) {
            element.element = new Stanza(name, namespacePrefix, element.attributes, element.innerFragments);
        } else {
            element.element = new XMLElement(name, namespacePrefix, element.attributes, element.innerFragments);
        }
        currentElement = element;
        
        // must be done after set as currentElement
        if(namespaceURI != null && namespaceURI.length() > 0) {
            if(namespacePrefix == null || namespacePrefix.length() == 0) {
                addNamespaceAttribute(namespaceURI);
            } else {
                addNamespaceAttribute(namespacePrefix, namespaceURI);
            }
        }
    }

    public StanzaBuilder addAttribute(String name, String value) {
        addAttribute(new Attribute(name, value));
        return this;
    }

    public StanzaBuilder addNamespaceAttribute(String value) {
        addAttribute(new NamespaceAttribute(value));
        return this;
    }

    public StanzaBuilder addNamespaceAttribute(String namespacePrefix, String value) {
        addAttribute(new NamespaceAttribute(namespacePrefix, value));
        return this;
    }

    public StanzaBuilder addAttribute(Attribute attribute) {
        checkReset();
        currentElement.attributes.add(attribute);
        return this;
    }

    public StanzaBuilder addText(String text) {
        checkReset();
        currentElement.innerFragments.add(new XMLText(text));
        return this;
    }

    public StanzaBuilder startInnerElement(String name) {
        return this.startInnerElement(name, null);
    }

    public StanzaBuilder startInnerElement(String name, String namespaceURI) {
        checkReset();

        startNewElement(name, namespaceURI, null, false);

        stack.peek().innerFragments.add(currentElement.element); // add new one to its parent

        stack.push(currentElement);

        return this;
    }

    public StanzaBuilder endInnerElement() {
        checkReset();
        if (stack.isEmpty()) throw new IllegalStateException("cannot end beyond top element");

        stack.pop(); // take current off stack and forget (it was added to its parent before)
        currentElement = stack.peek(); // we again deal with parent, which can be receive additions
        return this;
    }

    public StanzaBuilder addPreparedElement(XMLElement preparedElement) {
        checkReset();
        currentElement.innerFragments.add(preparedElement);
        return this;
    }

    /**
     * the stanza can only be retrieved once
     * @return retrieves the stanza and invalidates the builder
     */
    public Stanza getFinalStanza() {
        checkReset();
        Stanza returnStanza = resultingStanza;
        resultingStanza = null;
        isReset = true; // reset
        stack.clear();
        return returnStanza;
    }

    /**
     * assure that the immutable Stanza object is not changed after it was retrieved
     */
    private void checkReset() {
        if (isReset) throw new IllegalStateException("stanza builder was reset after retrieving stanza");
    }
}
