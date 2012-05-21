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

import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.ConferenceTestUtils;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 */
public class ChangeSubjectTestCase extends AbstractMUCMessageHandlerTestCase {

    private static final String SUBJECT = "Subject";

    public void testChangeSubjectNonModeratorAllowed() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2", RoomType.OpenSubject);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        assertNull(sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT));

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", null, SUBJECT, null, occupant1Queue
                .getNext());
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", null, SUBJECT, null, occupant2Queue
                .getNext());
    }

    public void testChangeSubject() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        room.addOccupant(OCCUPANT1_JID, "nick").setRole(Role.Moderator);
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        assertNull(sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT));

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", null, SUBJECT, null, occupant1Queue
                .getNext());
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", null, SUBJECT, null, occupant2Queue
                .getNext());
    }

    public void testChangeSubjectNonModerator() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM2_JID, "Room 2");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        Stanza error = sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT);

        assertMessageErrorStanza(error, ROOM2_JID, OCCUPANT1_JID, StanzaErrorType.AUTH, StanzaErrorCondition.FORBIDDEN, new XMLElementBuilder("subject")
                .addText(SUBJECT).build());

        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());
    }

}
