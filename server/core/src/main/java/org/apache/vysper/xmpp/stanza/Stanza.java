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

import java.util.List;
import java.util.Map;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;

/**
 * immutable container for all data contained in an XMPP stanza.
 * it is surrounded by a family of classes used to build, parse, verify and process stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Stanza extends XMLElement {

    public Stanza(String namespaceURI, String name, String namespacePrefix, List<Attribute> attributes,
            List<XMLFragment> innerFragments) {
        this(namespaceURI, name, namespacePrefix, attributes, innerFragments, null);
    }

    public Stanza(String namespaceURI, String name, String namespacePrefix, List<Attribute> attributes,
            List<XMLFragment> innerFragments, Map<String, String> namespaces) {
        super(namespaceURI, name, namespacePrefix, attributes, innerFragments, namespaces);
    }

    public Stanza(String namespaceURI, String name, String namespacePrefix, Attribute[] attributes,
            XMLFragment[] innerFragments) {
        this(namespaceURI, name, namespacePrefix, attributes, innerFragments, null);
    }

    public Stanza(String namespaceURI, String name, String namespacePrefix, Attribute[] attributes,
            XMLFragment[] innerFragments, Map<String, String> namespaces) {
        super(namespaceURI, name, namespacePrefix, attributes, innerFragments, namespaces);
    }

    public Entity getTo() {
        return parseEntityAttribute("to");
    }

    /**
     * Returns the from attribute <b>if</b> it is sent with the stanza (rare).
     * Use {@link XMPPCoreStanzaHandler#extractSenderJID()} to make sure you get
     * a JID (either with or without resource).
     * 
     * @return the sender JID, or null if not set.
     */
    public Entity getFrom() {
        return parseEntityAttribute("from");
    }

    public Entity parseEntityAttribute(String attributeName) {
        EntityImpl entity = null;
        String attributeValue = getAttributeValue(attributeName);
        if (attributeValue != null) {
            try {
                entity = EntityImpl.parse(attributeValue);
            } catch (EntityFormatException e) {
                return null;
            }
        }
        return entity;
    }

    @Override
    public String toString() {
        return DenseStanzaLogRenderer.render(this);
    }
}
