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

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.datetime.DateTimeProfile;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DiscussionMessageTestCase extends TestCase {

    private static final String NICK = "nick";

    private static final String BODY = "Body";

    private static final String SUBJECT = "Subject";

    private static final Calendar TIMESTAMP = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static final Entity FROM = EntityImpl.parseUnchecked("user@vysper.org/res");

    private static final Entity ROOM_JID = EntityImpl.parseUnchecked("room@vysper.org");

    private static final Room ROOM = new Room(ROOM_JID, "Room");
    
    private static final Occupant FROM_OCCUPANT = new Occupant(FROM, NICK, ROOM, Role.Visitor);

    public void testSubjectMessage() {
        StanzaBuilder builder = StanzaBuilder.createMessageStanza(FROM, ROOM_JID, null, null);
        builder.startInnerElement("subject", NamespaceURIs.JABBER_CLIENT).addText(SUBJECT).endInnerElement();
        final MessageStanza messageStanza = ConferenceTestUtils.toMessageStanza(builder.build());

        DiscussionMessage item = new DiscussionMessage(messageStanza, FROM_OCCUPANT, TIMESTAMP);
        assertEquals(NICK, item.getNick());
        assertEquals(TIMESTAMP, item.getTimestamp());
        assertFalse(item.hasBody());
        assertTrue(item.hasSubject());
    }

    public void testBodyMessage() {
        MessageStanza messageStanza = ConferenceTestUtils.createMessageStanza(FROM, ROOM_JID, BODY);

        DiscussionMessage item = new DiscussionMessage(messageStanza, FROM_OCCUPANT, TIMESTAMP);
        assertEquals(NICK, item.getNick());
        assertEquals(TIMESTAMP, item.getTimestamp());
        assertTrue(item.hasBody());
        assertFalse(item.hasSubject());
    }

    public void testCreateStanza() throws Exception {
        final MessageStanza inStanza = ConferenceTestUtils.createMessageStanza(FROM, ROOM_JID, BODY);
        DiscussionMessage item = new DiscussionMessage(inStanza, FROM_OCCUPANT, TIMESTAMP);

        Entity to = EntityImpl.parseUnchecked("user2@vysper.org/res");
        Occupant toOccupant = new Occupant(to, "nick 2", ROOM, Role.Visitor);
        MessageStanza outStanza = (MessageStanza)MessageStanza.getWrapper(item.createStanza(toOccupant, true));

        assertEquals(to, outStanza.getTo());
        assertEquals(new EntityImpl(ROOM_JID, NICK), outStanza.getFrom());
        assertEquals("groupchat", outStanza.getType());
        assertEquals(BODY, outStanza.getBody(null));

        XMLElement delayElm = outStanza.getInnerElements().get(1);
        assertEquals(FROM.getFullQualifiedName(), delayElm.getAttributeValue("from"));
        assertEquals(DateTimeProfile.getInstance().getDateTimeInUTC(TIMESTAMP.getTime()), delayElm
                .getAttributeValue("stamp"));

    }
}
