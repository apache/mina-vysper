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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCUserItem {

    protected Entity jid; // optional
    protected String nick; // optional
    protected Affiliation affiliation; // optional
    protected Role role; // optional

    public MUCUserItem(Occupant occupant) {
        this.jid = occupant.getJid();
        this.nick = occupant.getName();
        this.affiliation = occupant.getAffiliation();
        this.role = occupant.getRole();
    }

    
    public MUCUserItem(Entity jid, String nick, Affiliation affiliation, Role role) {
        this.jid = jid;
        this.nick = nick;
        this.affiliation = affiliation;
        this.role = role;
    }
    
    public Entity getJid() {
        return jid;
    }

    public String getNick() {
        return nick;
    }

    public Affiliation getAffiliation() {
        return affiliation;
    }

    public Role getRole() {
        return role;
    }
    
    public void insertElement(StanzaBuilder stanzaBuilder, boolean includeJid, boolean includeNick) {
        stanzaBuilder.startInnerElement("item");
            if (includeJid && jid != null) stanzaBuilder.addAttribute("jid", jid.getFullQualifiedName());
            if (includeNick && nick != null) stanzaBuilder.addAttribute("nick", nick);
            if (affiliation != null) stanzaBuilder.addAttribute("affiliation", affiliation.toString());
            if (role != null) stanzaBuilder.addAttribute("role", role.toString());
        stanzaBuilder.endInnerElement();
    }

}
