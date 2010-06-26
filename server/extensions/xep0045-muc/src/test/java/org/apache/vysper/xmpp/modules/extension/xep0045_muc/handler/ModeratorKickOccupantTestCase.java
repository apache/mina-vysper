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

import static org.apache.vysper.xmpp.stanza.IQStanzaType.SET;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserPresenceItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.IQStanza;

/**
 */
public class ModeratorKickOccupantTestCase extends AbstractMUCHandlerTestCase {

    public void testKickUser() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        room.addOccupant(OCCUPANT1_JID, "nick").setRole(Role.Moderator);
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        assertNotNull(room.findOccupantByNick("Nick 2"));

        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem("Nick 2", Role.None)));

        assertIqResultStanza(ROOM2_JID, OCCUPANT1_JID, "id1", result);

        assertNull(room.findOccupantByNick("Nick 2"));

        // verify that kicked user got presence
        assertPresenceStanza(occupant2Queue.getNext(), new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT2_JID,
                "unavailable", new MucUserPresenceItem(null, null, Affiliation.None, Role.None), StatusCode.BEEN_KICKED);

        // verify that remaining users got presence
        assertPresenceStanza(occupant1Queue.getNext(), new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT1_JID,
                "unavailable", new MucUserPresenceItem(null, null, Affiliation.None, Role.None), StatusCode.BEEN_KICKED);
    }

    public void testMemberAttemptKickAdmin() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setAffiliation(Affiliation.Member);
        occupant1.setRole(Role.Moderator);

        room.addOccupant(OCCUPANT2_JID, "Nick 2").setAffiliation(Affiliation.Admin);

        testKickNotAllowed(room, "Nick 2", "not-allowed");
    }

    public void testMemberAttemptKickOwner() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setAffiliation(Affiliation.Member);
        occupant1.setRole(Role.Moderator);

        room.addOccupant(OCCUPANT2_JID, "Nick 2").setAffiliation(Affiliation.Owner);

        testKickNotAllowed(room, "Nick 2", "not-allowed");
    }

    public void testAdminAttemptKickOwner() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setAffiliation(Affiliation.Admin);
        occupant1.setRole(Role.Moderator);

        room.addOccupant(OCCUPANT2_JID, "Nick 2").setAffiliation(Affiliation.Owner);

        testKickNotAllowed(room, "Nick 2", "not-allowed");
    }

    public void testToKickYourself() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setAffiliation(Affiliation.Admin);
        occupant1.setRole(Role.Moderator);

        room.addOccupant(OCCUPANT2_JID, "Nick 2").setAffiliation(Affiliation.Owner);

        testKickNotAllowed(room, "nick", "conflict");
    }

    
    private void testKickNotAllowed(Room room, String nickToKick, String expectedError) throws ProtocolException {
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem(nickToKick, Role.None)));

        XMLElement expectedInner = new XMLElementBuilder("query", NamespaceURIs.XEP0045_MUC_ADMIN).startInnerElement(
                "item", NamespaceURIs.XEP0045_MUC_ADMIN).addAttribute("nick", nickToKick).addAttribute("role", "none")
                .endInnerElement().build();

        assertErrorStanza(result, "iq", ROOM2_JID, OCCUPANT1_JID, "cancel", expectedError, expectedInner);

        assertNotNull(room.findOccupantByNick("nick"));
        assertNotNull(room.findOccupantByNick("Nick 2"));
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCIqAdminHandler(conference);
    }

}
