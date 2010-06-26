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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

public abstract class AbstractInviteDecline extends XMLElement {

    public AbstractInviteDecline(String elmName, XMLElement elm) {
        super(NamespaceURIs.XEP0045_MUC, elmName, null, elm.getAttributes(), elm.getInnerFragments());
    }

    public AbstractInviteDecline(String elmName, Entity from, Entity to, String reason) {
        super(NamespaceURIs.XEP0045_MUC, elmName, null, createAttributes(from, to), createFragments(reason));
    }

    private static List<Attribute> createAttributes(Entity from, Entity to) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (to != null)
            attributes.add(new Attribute("to", to.getFullQualifiedName()));
        if (from != null)
            attributes.add(new Attribute("from", from.getFullQualifiedName()));
        return attributes;
    }

    private static List<XMLFragment> createFragments(String reason) {
        List<XMLFragment> fragments = new ArrayList<XMLFragment>();
        if (reason != null) {
            XMLElement reasonElm = new XMLElement(NamespaceURIs.XEP0045_MUC, "reason", null, null,
                    new XMLFragment[] { new XMLText(reason) });
            fragments.add(reasonElm);
        }
        return fragments;
    }

    public Entity getFrom() throws EntityFormatException {
        String value = getAttributeValue("from");
        if (value != null) {
            return EntityImpl.parse(value);
        } else {
            return null;
        }
    }

    public Entity getTo() throws EntityFormatException {
        String value = getAttributeValue("to");
        if (value != null) {
            return EntityImpl.parse(value);
        } else {
            return null;
        }
    }

    public String getReason() {
        try {
            XMLElement reasonElm = getSingleInnerElementsNamed("reason");
            if (reasonElm != null && reasonElm.getInnerText() != null) {
                return reasonElm.getInnerText().getText();
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }
}
