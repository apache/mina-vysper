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

import java.util.Arrays;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.RecordingStanzaBroker;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class ChangeNickTestCase extends AbstractMUCHandlerTestCase {

    private Stanza changeNick(Entity occupantJid, Entity roomWithNickJid) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomWithNickJid, null, null, null,
                null);
        stanzaBuilder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);

        stanzaBuilder.endInnerElement();
        Stanza presenceStanza = stanzaBuilder.build();

        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker(
                new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        handler.execute(presenceStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker);
        return stanzaBroker.getUniqueStanzaWrittenToSession();
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCPresenceHandler(conference);
    }

    public void testChangeNick() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        changeNick(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "new nick"));

        Occupant occupant = room.findOccupantByJID(OCCUPANT1_JID);
        assertEquals("new nick", occupant.getNick());

        MucUserItem unavailbleItem = new MucUserItem(OCCUPANT1_JID, "new nick", Affiliation.None, Role.Participant);
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "nick"), OCCUPANT1_JID, PresenceStanzaType.UNAVAILABLE,
                Arrays.asList(unavailbleItem), Arrays.asList(StatusCode.NEW_NICK, StatusCode.OWN_PRESENCE),
                occupant1Queue.getNext());
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "nick"), OCCUPANT2_JID, PresenceStanzaType.UNAVAILABLE,
                Arrays.asList(unavailbleItem), Arrays.asList(StatusCode.NEW_NICK), occupant2Queue.getNext());

        MucUserItem availbleItem = new MucUserItem(OCCUPANT1_JID, null, Affiliation.None, Role.Participant);
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "new nick"), OCCUPANT1_JID, null, Arrays.asList(availbleItem),
                Arrays.asList(StatusCode.OWN_PRESENCE), occupant1Queue.getNext());
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "new nick"), OCCUPANT2_JID, null, Arrays.asList(availbleItem),
                null, occupant2Queue.getNext());
    }

    public void testChangeNickWithDuplicateNick() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        Stanza error = changeNick(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "nick 2"));

        assertNotNull(error);
    }

}
