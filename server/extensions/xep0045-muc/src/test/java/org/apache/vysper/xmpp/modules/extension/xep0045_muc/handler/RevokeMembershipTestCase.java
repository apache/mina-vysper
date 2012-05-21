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

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.ConferenceTestUtils;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;

/**
 */
public class RevokeMembershipTestCase extends AbstractAffiliationTestCase {

    public void testRevokeMembershipByNick() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2", RoomType.MembersOnly);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);
        
        Occupant occ1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occ1.setRole(Role.Moderator);
        
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Member);
        Occupant occ2 = room.addOccupant(OCCUPANT2_JID, "Nick 2");
        occ2.setRole(Role.Visitor);

        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem("Nick 2", Affiliation.None)));
        assertIqResultStanza(ROOM2_JID, OCCUPANT1_JID, "id1", result);

        assertEquals(Affiliation.None, room.getAffiliations().getAffiliation(OCCUPANT2_JID));

        // verify that remaining users got presence
        MucUserItem expectedMucPresenceItem = new MucUserItem(null, null, Affiliation.None, Role.None);
        assertPresenceStanza(new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT2_JID, PresenceStanzaType.UNAVAILABLE,
                expectedMucPresenceItem, StatusCode.REMOVED_BY_AFFILIATION, occupant2Queue.getNext());
        assertPresenceStanza(new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT1_JID, PresenceStanzaType.UNAVAILABLE,
                expectedMucPresenceItem, StatusCode.REMOVED_BY_AFFILIATION, occupant1Queue.getNext());
    }

    // Test granting membership for a user which is not currently in the room
    public void testRevokeMembershipByJid() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occ1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occ1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);
        
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Member);
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem(OCCUPANT2_JID, Affiliation.None)));
        assertIqResultStanza(ROOM2_JID, OCCUPANT1_JID, "id1", result);

        assertEquals(Affiliation.None, room.getAffiliations().getAffiliation(OCCUPANT2_JID));

        // verify that remaining users got message
        // must be sent from the room
        assertMessageStanza(ROOM2_JID, OCCUPANT1_JID,
                null, new MucUserItem(OCCUPANT2_JID, null, Affiliation.None, Role.None), occupant1Queue.getNext());
    }

    public void testNonAdmin() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Member);

        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        assertChangeNotAllowed("Nick 2", StanzaErrorCondition.NOT_ALLOWED, Affiliation.None, null);
    }


}
