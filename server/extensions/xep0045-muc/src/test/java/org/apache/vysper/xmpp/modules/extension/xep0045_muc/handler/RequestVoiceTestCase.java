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

import static org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type.submit;
import static org.apache.vysper.xmpp.stanza.dataforms.Field.Type.TEXT_SINGLE;

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.dataforms.VoiceRequestForm;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.ConferenceTestUtils;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
/**
 */
public class RequestVoiceTestCase extends AbstractMUCMessageHandlerTestCase {

    public void testRequestVoice() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        Occupant moderator = room.addOccupant(OCCUPANT1_JID, "nick");
        moderator.setRole(Role.Moderator);
        
        Occupant requestor = room.addOccupant(OCCUPANT2_JID, "Nick 2");
        requestor.setRole(Role.Visitor);
        
        DataForm form = new DataForm();
        form.setType(submit);
        form.addField(new Field(null, null, "FORM_TYPE", NamespaceURIs.XEP0045_MUC_REQUEST));

        form.addField(new Field("Requested role", TEXT_SINGLE, "muc#role", "participant"));

        sendMessage(OCCUPANT2_JID, ROOM1_JID, new DataFormEncoder().getXML(form));

        Stanza request = occupant1Queue.getNext();
        assertMessageStanza(ROOM1_JID, OCCUPANT1_JID, null, new VoiceRequestForm(OCCUPANT2_JID, "Nick 2").createFormXML(), request);
    }

    public void testGrantVoice() throws Exception {
        Room room = ConferenceTestUtils.findOrCreateRoom(conference, ROOM1_JID, "Room 1");
        Occupant moderator = room.addOccupant(OCCUPANT1_JID, "nick");
        moderator.setRole(Role.Moderator);
        
        Occupant requestor = room.addOccupant(OCCUPANT2_JID, "Nick 2");
        requestor.setRole(Role.Visitor);
        
        DataForm form = new DataForm();
        form.setType(submit);
        form.addField(new Field(null, null, "FORM_TYPE", NamespaceURIs.XEP0045_MUC_REQUEST));
        form.addField(new Field(null, null, "muc#role", "participant"));
        form.addField(new Field(null, null, "muc#jid", OCCUPANT2_JID.getFullQualifiedName()));
        form.addField(new Field(null, null, "muc#roomnick", "Nick 2"));
        form.addField(new Field(null, null, "muc#request_allow", "true"));

        sendMessage(OCCUPANT1_JID, ROOM1_JID, new DataFormEncoder().getXML(form));

        assertEquals(Role.Participant, room.findOccupantByNick("Nick 2").getRole());

        // verify that remaining users got presence
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "Nick 2"), OCCUPANT2_JID, null,
                new MucUserItem(null, null, Affiliation.None, Role.Participant), null, occupant2Queue.getNext());
        assertPresenceStanza(new EntityImpl(ROOM1_JID, "Nick 2"), OCCUPANT1_JID, null,
                new MucUserItem(null, null, Affiliation.None, Role.Participant), null, occupant1Queue.getNext());
    }
}
