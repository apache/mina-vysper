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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

/**
 * An occupant (user) in a room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Occupant {

    private Room room;

    private Role role;

    private Entity jid;

    private String nick;

    public Occupant(Entity jid, String nick, Room room, Role role) {
        if (jid == null)
            throw new IllegalArgumentException("JID can not be null");
        if (nick == null)
            throw new IllegalArgumentException("Name can not be null");
        if (room == null)
            throw new IllegalArgumentException("Room can not be null");
        if (role == null)
            throw new IllegalArgumentException("Role can not be null");

        this.jid = jid;
        this.nick = nick;
        this.room = room;
        this.role = role;
    }

    public Affiliation getAffiliation() {
        Affiliation affiliation = room.getAffiliations().getAffiliation(jid);
        if(affiliation == null) affiliation = Affiliation.None;
        return affiliation;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Entity getJid() {
        return jid;
    }

    public boolean hasVoice() {
        if (role == Role.Moderator || role == Role.Participant) return true;
        if (room.isRoomType(RoomType.Unmoderated) && room.doVisitorsHaveVoice()) return true;
        return false;
    }

    @Override
    public String toString() {
        return jid.getFullQualifiedName();
    }

    public boolean isModerator() {
        return role == Role.Moderator;
    }
    
    public Entity getJidInRoom() {
        return new EntityImpl(room.getJID(), nick);
    }

}
