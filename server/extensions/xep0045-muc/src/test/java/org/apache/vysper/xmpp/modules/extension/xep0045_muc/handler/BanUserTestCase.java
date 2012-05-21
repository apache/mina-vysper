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
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.StanzaAssert;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.ConferenceTestUtils;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.junit.Assert;

/**
 */
public class BanUserTestCase extends AbstractAffiliationTestCase {

    public void testBanUser() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occ1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occ1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);
        
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Member);
        room.addOccupant(OCCUPANT2_JID, "Nick 2");
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem(OCCUPANT2_JID, Affiliation.Outcast)));
        
        assertIqResultStanza(ROOM2_JID, OCCUPANT1_JID, "id1", result);

        assertNull(room.findOccupantByNick("Nick 2"));
        assertEquals(Affiliation.Outcast, room.getAffiliations().getAffiliation(OCCUPANT2_JID));

        // banned user is notified
        assertPresenceStanza(new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT2_JID, PresenceStanzaType.UNAVAILABLE,
                new MucUserItem(null, null, Affiliation.Outcast, Role.None), StatusCode.BEEN_BANNED, occupant2Queue.getNext());
        
        // verify that remaining users got message
        // must be sent from the room
        assertPresenceStanza(new EntityImpl(ROOM2_JID, "Nick 2"), OCCUPANT1_JID, PresenceStanzaType.UNAVAILABLE,
                new MucUserItem(null, null, Affiliation.Outcast, Role.None), StatusCode.BEEN_BANNED, occupant1Queue.getNext());
    }

    public void testNonAdmin() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Member);

        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        assertChangeNotAllowed("Nick 2", StanzaErrorCondition.NOT_ALLOWED, Affiliation.None, null);
    }

    public void testAdminBanningOwner() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occupant1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occupant1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);

        room.addOccupant(OCCUPANT2_JID, "Nick 2");
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Owner);

        assertChangeNotAllowed("Nick 2", StanzaErrorCondition.NOT_ALLOWED, Affiliation.Outcast, null);
    }

    public void testGetBanList() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occ1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occ1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);
        
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Member);
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        room.getAffiliations().add(OCCUPANT3_JID, Affiliation.Outcast);
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, IQStanzaType.GET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem((String)null, Affiliation.Outcast)));
        
        Stanza expected = StanzaBuilder.createIQStanza(ROOM2_JID, OCCUPANT1_JID, IQStanzaType.RESULT, "id1")
            .startInnerElement("query", NamespaceURIs.XEP0045_MUC_ADMIN)
            .startInnerElement("item", NamespaceURIs.XEP0045_MUC_ADMIN)
            .addAttribute("affiliation", "outcast")
            .addAttribute("jid", OCCUPANT3_JID.getFullQualifiedName())
            .build();
            
        StanzaAssert.assertEquals(expected, result);
    }

    public void testGetBanListNonAdmin() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Member);
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, IQStanzaType.GET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem((String)null, Affiliation.Outcast)));
        
        XMLElementBuilder builder = new XMLElementBuilder("query", NamespaceURIs.XEP0045_MUC_ADMIN).startInnerElement(
                "item", NamespaceURIs.XEP0045_MUC_ADMIN).addAttribute("affiliation", Affiliation.Outcast.toString());
        XMLElement expectedInner = builder.build();
        
        assertErrorStanza(result, "iq", ROOM2_JID, OCCUPANT1_JID, StanzaErrorType.CANCEL, StanzaErrorCondition.NOT_ALLOWED, expectedInner);
    }

    public void testSetBanList() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        Occupant occ1 = room.addOccupant(OCCUPANT1_JID, "nick");
        occ1.setRole(Role.Moderator);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);
        
        room.getAffiliations().add(OCCUPANT2_JID, Affiliation.Member);
        room.getAffiliations().add(OCCUPANT3_JID, Affiliation.Outcast);
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, IQStanzaType.SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, 
                new IqAdminItem(OCCUPANT2_JID, Affiliation.Outcast),
                new IqAdminItem(OCCUPANT3_JID, Affiliation.None)
            ));
        
        Stanza expected = StanzaBuilder.createIQStanza(ROOM2_JID, OCCUPANT1_JID, IQStanzaType.RESULT, "id1")
            .build();
            
        StanzaAssert.assertEquals(expected, result);
        
        Assert.assertEquals(Affiliation.Outcast, room.getAffiliations().getAffiliation(OCCUPANT2_JID));
        Assert.assertEquals(Affiliation.None, room.getAffiliations().getAffiliation(OCCUPANT3_JID));
    }

}
