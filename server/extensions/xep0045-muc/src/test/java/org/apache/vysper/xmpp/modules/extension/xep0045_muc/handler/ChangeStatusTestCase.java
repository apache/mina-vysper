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

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.RecordingStanzaBroker;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class ChangeStatusTestCase extends AbstractMUCHandlerTestCase {

    private Stanza changeStatus(Entity occupantJid, Entity roomWithNickJid, String show, String status)
            throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomWithNickJid, null, null, show,
                status);

        Stanza presenceStanza = stanzaBuilder.build();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker(new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        handler.execute(presenceStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker);
        return stanzaBroker.getUniqueStanzaWrittenToSession();
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCPresenceHandler(conference);
    }

    public void testChangeShowStatus() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        assertNull(changeStatus(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, "xa", "Gone"));

        MucUserItem item = new MucUserItem(OCCUPANT1_JID, "nick", Affiliation.None, Role.Participant);
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT1_JID, "xa", "Gone", item, occupant1Queue.getNext());
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "xa", "Gone", item, occupant2Queue.getNext());
    }

    public void testChangeShow() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        assertNull(changeStatus(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, "xa", null));

        MucUserItem item = new MucUserItem(OCCUPANT1_JID, "nick", Affiliation.None, Role.Participant);
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT1_JID, "xa", null, item, occupant1Queue.getNext());
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "xa", null, item, occupant2Queue.getNext());
    }

    public void testChangeStatus() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        assertNull(changeStatus(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, null, "Gone"));

        MucUserItem item = new MucUserItem(OCCUPANT1_JID, "nick", Affiliation.None, Role.Participant);
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT1_JID, null, "Gone", item, occupant1Queue.getNext());
        assertPresenceStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, null, "Gone", item, occupant2Queue.getNext());
    }

    private void assertPresenceStanza(Entity expectedFrom, Entity expectedTo, String expectedShow,
            String expectedStatus, MucUserItem expectedItem, Stanza actualStanza) throws XMLSemanticError, Exception {

        assertPresenceStanza(expectedFrom, expectedTo, null, expectedShow, expectedStatus, Arrays.asList(expectedItem),
                null, actualStanza);
    }
}
