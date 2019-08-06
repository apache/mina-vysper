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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xml.fragment.AbstractXMLElementBuilder;
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.commandstanza.EndOfSessionCommandStanza;
import org.apache.vysper.xmpp.server.SessionContext;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaBuilder extends AbstractXMLElementBuilder<StanzaBuilder, Stanza> {

    public static StanzaBuilder createIQStanza(Entity from, Entity to, IQStanzaType type, String id) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        if (from != null)
            stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        if (to != null)
            stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        stanzaBuilder.addAttribute("type", type.value());
        stanzaBuilder.addAttribute("id", id);
        return stanzaBuilder;
    }

    public static StanzaBuilder createMessageStanza(Entity from, Entity to, String lang, String body) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("message", NamespaceURIs.JABBER_CLIENT);
        if (from != null) {
            stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        }
        stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        if (lang != null)
            stanzaBuilder.addAttribute(NamespaceURIs.XML, "lang", lang);
        if (body != null)
            stanzaBuilder.startInnerElement("body", NamespaceURIs.JABBER_CLIENT).addText(body).endInnerElement();
        return stanzaBuilder;
    }

    public static StanzaBuilder createMessageStanza(Entity from, Entity to, MessageStanzaType type, String lang,
            String body) {
        StanzaBuilder stanzaBuilder = createMessageStanza(from, to, lang, body);
        if (type != null)
            stanzaBuilder.addAttribute("type", type.value());
        return stanzaBuilder;
    }

    public static Stanza createUnavailablePresenceStanza(String status,
            SessionContext.SessionTerminationCause terminationCause) {
        StanzaBuilder presenceUnavailBuilder = createPresenceStanza(null, null, null, PresenceStanzaType.UNAVAILABLE,
                null, status);
        if (terminationCause == null) {
            return presenceUnavailBuilder.build();
        } else {
            return new EndOfSessionCommandStanza(presenceUnavailBuilder.build(), terminationCause);
        }
    }

    public static StanzaBuilder createPresenceStanza(Entity from, Entity to, String lang, PresenceStanzaType type,
            String show, String status) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("presence", NamespaceURIs.JABBER_CLIENT);
        if (from != null)
            stanzaBuilder.addAttribute("from", from.getFullQualifiedName());
        if (to != null)
            stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        if (lang != null)
            stanzaBuilder.addAttribute(NamespaceURIs.XML, "lang", lang);
        if (type != null)
            stanzaBuilder.addAttribute("type", type.value());
        if (show != null) {
            stanzaBuilder.startInnerElement("show", NamespaceURIs.JABBER_CLIENT).addText(show).endInnerElement();
        }
        if (status != null) {
            stanzaBuilder.startInnerElement("status", NamespaceURIs.JABBER_CLIENT).addText(status).endInnerElement();
        }
        return stanzaBuilder;
    }

    public static StanzaBuilder createDirectReply(XMPPCoreStanza original, boolean fromIsServerOnly, IQStanzaType type) {
        return createDirectReply(original, fromIsServerOnly, type == null ? null : type.value());
    }

    public static StanzaBuilder createDirectReply(XMPPCoreStanza original, boolean fromIsServerOnly, String type) {
        if (original == null)
            throw new IllegalArgumentException();

        StanzaBuilder stanzaBuilder = new StanzaBuilder(original.getName(), original.getNamespaceURI(), original
                .getNamespacePrefix());
        // reverse to and from
        Entity newTo = original.getFrom();
        if (newTo != null) {
            stanzaBuilder.addAttribute("to", newTo.getFullQualifiedName());
        }
        Entity newFrom = original.getTo();
        if (newFrom != null) {
            if (fromIsServerOnly)
                newFrom = new EntityImpl(null, newFrom.getDomain(), null);
            stanzaBuilder.addAttribute("from", newFrom.getFullQualifiedName());
        }
        stanzaBuilder.addAttribute("type", type);
        if (original.getID() != null)
            stanzaBuilder.addAttribute("id", original.getID());

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
        StanzaBuilder stanzaBuilder = new StanzaBuilder(original.getName(), original.getNamespaceURI(), original
                .getNamespacePrefix());

        List<Attribute> replacingAttributesCopy = new ArrayList<Attribute>();
        if (replacingAttributes != null)
            replacingAttributesCopy.addAll(replacingAttributes);

        List<Attribute> originalAttributes = original.getAttributes();
        for (Attribute originalAttribute : originalAttributes) {
            boolean wasReplaced = false;
            for (Iterator<Attribute> it = replacingAttributesCopy.iterator(); it.hasNext();) {
                Attribute replacingAttribute = it.next();
                if (replacingAttribute == null)
                    continue;
                if (replacingAttribute.getName().equals(originalAttribute.getName())) {
                    stanzaBuilder.addAttribute(replacingAttribute);
                    it.remove(); // this has been processed
                    wasReplaced = true;
                    break;
                }
            }
            if (!wasReplaced)
                stanzaBuilder.addAttribute(originalAttribute);
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

    /**
     * creates a new stanza which only differs from the given original by 'from' and 'to' attributes. 
     * 
     * @param original 
     * @param from if NOT NULL, the new 'from'
     * @param to if NOT NULL, the new 'to'
     * @return stanza builder with to and from replaced
     */
    public static StanzaBuilder createForward(Stanza original, Entity from, Entity to) {
        List<Attribute> toFromReplacements = new ArrayList<Attribute>(2);
        if (to != null)
            toFromReplacements.add(new Attribute("to", to.getFullQualifiedName()));
        if (from != null)
            toFromReplacements.add(new Attribute("from", from.getFullQualifiedName()));

        return createClone(original, true, toFromReplacements);
    }

    /**
     * convenience shortcut for {@link #createForward(Stanza, org.apache.vysper.xmpp.addressing.Entity, org.apache.vysper.xmpp.addressing.Entity)}
     * 
     * @param original 
     * @param from if NOT NULL, the new 'from'
     * @param to if NOT NULL, the new 'to'
     * @return forward stanza
     */
    public static Stanza createForwardStanza(Stanza original, Entity from, Entity to) {
        return createForward(original, from, to).build();
    }
    
    public static Stanza rewriteNamespace(Stanza stanza, String fromNamespaceUri, String toNamespaceUri) {
        StanzaBuilder builder = new StanzaBuilder(stanza.getName(), toNamespaceUri, stanza.getNamespacePrefix());
        for(Attribute attribute : stanza.getAttributes()) {
            builder.addAttribute(attribute);
        }
        
        for(XMLFragment fragment : stanza.getInnerFragments()) {
            if(fragment instanceof XMLElement) {
                rewriteNamespace((XMLElement) fragment, builder, fromNamespaceUri, toNamespaceUri);
            } else {
                // XMLText
                builder.addText(((XMLText)fragment).getText());
            }
        }
        
        return builder.build();
    }
    
    private static void rewriteNamespace(XMLElement element, StanzaBuilder builder, String fromNamespaceUri, String toNamespaceUri) {
        if(fromNamespaceUri.equals(element.getNamespaceURI())) {
            builder.startInnerElement(element.getName(), toNamespaceUri);
            for(Attribute attribute : element.getAttributes()) {
                builder.addAttribute(attribute);
            }
            for(XMLFragment fragment : element.getInnerFragments()) {
                if(fragment instanceof XMLElement) {
                    rewriteNamespace((XMLElement) fragment, builder, fromNamespaceUri, toNamespaceUri);
                } else {
                    // XMLText
                    builder.addText(((XMLText)fragment).getText());
                }
            }
            builder.endInnerElement();
        } else {
            builder.addPreparedElement(element);
        }
    }

    static class ElementStruct {
        public ElementStruct parentElement = null;

        public XMLElement element = null;

        public List<Attribute> attributes = null;

        public List<XMLFragment> innerFragments = null;
    }

    public StanzaBuilder(String stanzaName) {
        this(stanzaName, null);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI) {
        this(stanzaName, namespaceURI, null);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI, String namespacePrefix) {
        super(stanzaName, namespaceURI, namespacePrefix);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI, String namespacePrefix, List<Attribute> attributes,
            List<XMLFragment> innerFragments) {
        super(stanzaName, namespaceURI, namespacePrefix, attributes, null, innerFragments);
    }

    public StanzaBuilder(String stanzaName, String namespaceURI, String namespacePrefix, List<Attribute> attributes,
            Map<String, String> namespaces, List<XMLFragment> innerFragments) {
        super(stanzaName, namespaceURI, namespacePrefix, attributes, namespaces, innerFragments);
    }

    protected XMLElement createElement(String namespaceURI, String name, String namespacePrefix,
            List<Attribute> attributes, Map<String, String> namespaces, List<XMLFragment> innerFragments) {
        // when creating the first element, make it a stanza
        if (currentElement == null) {
            return new Stanza(namespaceURI, name, namespacePrefix, attributes, innerFragments, namespaces);
        } else {
            return new XMLElement(namespaceURI, name, namespacePrefix, attributes, innerFragments, namespaces);
        }
    }
}
