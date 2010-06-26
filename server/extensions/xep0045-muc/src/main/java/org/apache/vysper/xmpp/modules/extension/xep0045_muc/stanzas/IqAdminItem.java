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
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class IqAdminItem extends XMLElement {

    public static List<IqAdminItem> extractItems(IQStanza stanza) throws XMLSemanticError {
        XMLElement query = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0045_MUC_ADMIN);
        List<XMLElement> itemElms = query.getInnerElementsNamed("item", NamespaceURIs.XEP0045_MUC_ADMIN);

        List<IqAdminItem> items = new ArrayList<IqAdminItem>();
        for (XMLElement itemElm : itemElms) {
            items.add(getWrapper(itemElm));
        }
        return items;
    }

    public static IqAdminItem getWrapper(XMLElement itemElm) {
        String nick = itemElm.getAttributeValue("nick");
        String roleStr = itemElm.getAttributeValue("role");
        Role role = null;
        if (roleStr != null) {
            role = Role.fromString(roleStr);
        }

        return new IqAdminItem(nick, role);
    }

    public IqAdminItem(String nick, Role role) {
        super(NamespaceURIs.XEP0045_MUC_ADMIN, "item", null, createAttributes(nick, role), null);
    }

    public String getNick() {
        return getAttributeValue("nick");
    }

    public Role getRole() {
        String value = getAttributeValue("role");
        if (value != null) {
            return Role.fromString(value);
        } else {
            return null;
        }
    }

    private static List<Attribute> createAttributes(String nick, Role role) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (nick != null)
            attributes.add(new Attribute("nick", nick));
        if (role != null)
            attributes.add(new Attribute("role", role.toString()));
        return attributes;
    }

}
