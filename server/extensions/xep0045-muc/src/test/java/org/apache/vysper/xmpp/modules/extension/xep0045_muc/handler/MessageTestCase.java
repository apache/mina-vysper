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

import static org.apache.vysper.xmpp.stanza.MessageStanzaType.GROUPCHAT;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.ConferenceTestUtils;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Decline;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Invite;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Password;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 */
public class MessageTestCase extends AbstractMUCMessageHandlerTestCase {

    public void testMessageWithNoVoice() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        Occupant occupant = room.addOccupant(OCCUPANT1_JID, "nick");
        // make sure the occupant has no voice
        occupant.setRole(Role.Visitor);

        testNotAllowedMessage(room, StanzaErrorCondition.FORBIDDEN);
    }

    public void testMessageUserNotOccupant() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        // do not add user to room

        testNotAllowedMessage(room, StanzaErrorCondition.NOT_ACCEPTABLE);
    }

    public void testMessageToRoomWithRelays() throws Exception {
        String body = "Message body";

        // add occupants to the room
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        sendMessage(OCCUPANT1_JID, ROOM1_JID, GROUPCHAT, body);

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", body, occupant1Queue.getNext());
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", body, occupant2Queue.getNext());
    }

    public void testMessageToOccupant() throws Exception {
        String body = "Message body";

        // add occupants to the room
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to occupant 1
        sendMessage(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "Nick 2"), MessageStanzaType.CHAT, body);

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "chat", body, occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }

    public void testGroupChatMessageToOccupant() throws Exception {
        // add occupants to the room
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to occupant 1 with type groupchat
        Stanza errorStanza = sendMessage(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "Nick 2"),
                MessageStanzaType.GROUPCHAT, BODY);

        XMLElement expectedBody = new XMLElementBuilder("body").addText(BODY).build();
        assertMessageErrorStanza(errorStanza, ROOM1_JID, OCCUPANT1_JID, StanzaErrorType.MODIFY, StanzaErrorCondition.BAD_REQUEST, expectedBody);

        // no message should be relayed
        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());

    }

    public void testInviteMessageWithPassword() throws Exception {
        String reason = "Join me!";

        // add occupants to the room
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        room.setPassword("secret");
        room.addOccupant(OCCUPANT1_JID, "nick");

        Invite invite = new Invite(null, OCCUPANT2_JID, reason);
        // send message to occupant 1
        assertNull(sendMessage(OCCUPANT1_JID, ROOM1_JID, null, null, new X(NamespaceURIs.XEP0045_MUC_USER, invite), null));

        X expectedX = new X(new Invite(OCCUPANT1_JID, null, reason), new Password("secret"));
        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID, OCCUPANT2_JID, null, null, null, expectedX, occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }

    public void testDeclineMessage() throws Exception {
        String reason = "No way";

        // add occupants to the room
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT2_JID, "nick");

        Decline decline = new Decline(null, OCCUPANT2_JID, reason);
        // send message to occupant 1
        Stanza error = sendMessage(OCCUPANT1_JID, ROOM1_JID, null, null, new X(NamespaceURIs.XEP0045_MUC_USER, decline), null);
        assertNull(error);

        X expectedX = new X(new Decline(OCCUPANT1_JID, null, reason));
        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID, OCCUPANT2_JID, null, null, null, expectedX, occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }
}
