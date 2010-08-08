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
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MucUserItem extends XMLElement {

    public MucUserItem(Occupant occupant, boolean includeJid, boolean includeNick) {
        super(NamespaceURIs.XEP0045_MUC_USER, "item", null, createAttributes(occupant, includeJid, includeNick), null);
    }

    public MucUserItem(Affiliation affiliation, Role role) {
        super(NamespaceURIs.XEP0045_MUC_USER, "item", null, createAttributes(null, null, affiliation, role), null);
    }

    public MucUserItem(Entity jid, String nick, Affiliation affiliation, Role role) {
        super(NamespaceURIs.XEP0045_MUC_USER, "item", null, createAttributes(jid, nick, affiliation, role), null);
    }

    private static List<Attribute> createAttributes(Occupant occupant, boolean includeJid, boolean includeNick) {
        Entity jid = includeJid ? occupant.getJid() : null;
        String nick = includeNick ? occupant.getNick() : null;

        return createAttributes(jid, nick, occupant.getAffiliation(), occupant.getRole());
    }

    public Entity getJid() throws EntityFormatException {
        String value = getAttributeValue("jid");
        if (value != null) {
            return EntityImpl.parse(value);
        } else {
            return null;
        }
    }

    public String getNick() {
        return getAttributeValue("nick");
    }

    public Affiliation getAffiliation() {
        String value = getAttributeValue("affiliation");
        if (value != null) {
            return Affiliation.fromString(value);
        } else {
            return null;
        }

    }

    public Role getRole() {
        String value = getAttributeValue("role");
        if (value != null) {
            return Role.fromString(value);
        } else {
            return null;
        }
    }

    private static List<Attribute> createAttributes(Entity jid, String nick, Affiliation affiliation, Role role) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (jid != null)
            attributes.add(new Attribute("jid", jid.getFullQualifiedName()));
        if (nick != null)
            attributes.add(new Attribute("nick", nick));
        if (affiliation != null)
            attributes.add(new Attribute("affiliation", affiliation.toString()));
        if (role != null)
            attributes.add(new Attribute("role", role.toString()));
        return attributes;
    }

}
