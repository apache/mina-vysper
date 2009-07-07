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
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;

import java.util.List;

/**
 * immutable container for all data contained in an XMPP stanza.
 * it is surrounded by a family of classes used to build, parse, verify and process stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Stanza extends XMLElement {

    public Stanza(String name, String namespace, List<Attribute> attributes, List<XMLFragment> innerFragments) {
        super(name, namespace, attributes, innerFragments);
    }

    public Stanza(String name, String namespace, Attribute[] attributes, XMLFragment[] innerFragments) {
        super(name, namespace, attributes, innerFragments);
    }

    public Stanza(String name, String namespace, List<Attribute> attributes, XMLFragment[] innerFragments) {
        super(name, namespace, attributes, innerFragments);
    }

    public Stanza(String name, String namespace, Attribute[] attributes, List<XMLFragment> innerFragments) {
        super(name, namespace, attributes, innerFragments);
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
        boolean isPresent = getVerifier().attributePresent(attributeName);
        EntityImpl entity = null;
        if (isPresent) {
            String attributeValue = getAttributeValue(attributeName);
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
