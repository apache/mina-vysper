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
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DiscussionHistoryTestCase extends TestCase {

    private static final String NICK = "nick";

    private static final String BODY = "Body";

    private static final String SUBJECT = "Subject";

    private static final Entity FROM = EntityImpl.parseUnchecked("user@vysper.org/res");

    private static final Entity RECEIVER = EntityImpl.parseUnchecked("user2@vysper.org/res");

    private static final Entity ROOM_JID = EntityImpl.parseUnchecked("room@vysper.org");

    private static final Room ROOM = new Room(ROOM_JID, "Room");

    private static final Occupant FROM_OCCUPANT = new Occupant(FROM, NICK, ROOM, Role.Visitor);

    private static final Occupant RECEIVER_OCCUPANT = new Occupant(RECEIVER, "nick2", ROOM, Role.Visitor);

    private DiscussionHistory history;

    private Calendar createTimestamp(int minutesAgo) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MINUTE, -minutesAgo);
        return cal;
    }

    @Override
    protected void setUp() throws Exception {
        history = new DiscussionHistory();

        // add some messages to the history, one more than is handled
        int maxStanzas = DiscussionHistory.DEFAULT_HISTORY_SIZE + 1;
        for (int i = 0; i < maxStanzas; i++) {
            history.append(ConferenceTestUtils.createMessageStanza(FROM, ROOM_JID, BODY + i), 
                           FROM_OCCUPANT, createTimestamp(maxStanzas - i));
        }

        // add a subject message
        final Stanza stanza = StanzaBuilder.createMessageStanza(FROM, ROOM_JID, MessageStanzaType.GROUPCHAT, null, null)
                .startInnerElement("subject", NamespaceURIs.JABBER_CLIENT).addText(SUBJECT).endInnerElement().build();
        history.append(ConferenceTestUtils.toMessageStanza(stanza), FROM_OCCUPANT);
    }

    public void testGetAllStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(null, null, null, null));

        assertStanzas(stanzas, DiscussionHistory.DEFAULT_HISTORY_SIZE);
    }

    public void testGetAllStanzasNullHistory() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, null);

        assertStanzas(stanzas, DiscussionHistory.DEFAULT_HISTORY_SIZE);
    }

    public void testThreeStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(3, null, null, null));
        assertStanzas(stanzas, 3);
    }

    public void testZeroStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(0, null, null, null));

        assertStanzas(stanzas, 0);
    }

    public void test500CharStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(null, 500, null, null));

        // 2 stanzas should fit in 500 chars
        assertStanzas(stanzas, 2);
    }

    public void test0CharStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(null, 0, null, null));

        assertStanzas(stanzas, 0);
    }

    public void test150SecondsStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(null, null, 150, null));

        // 2 stanzas + subject should fit in 150 seconds
        assertStanzas(stanzas, 3);
    }

    public void testSince5minutesStanzas() throws Exception {
        List<Stanza> stanzas = history.createStanzas(RECEIVER_OCCUPANT, true, new History(null, null, null,
                createTimestamp(5)));

        // 2 stanzas + subject should fit in 150 seconds
        assertStanzas(stanzas, 6);
    }

    private void assertStanzas(List<Stanza> stanzas, int expectedSize) throws Exception {
        assertEquals(expectedSize, stanzas.size());

        if (expectedSize > 0) {
            for (int i = 0; i < expectedSize - 1; i++) {
                Stanza stanza = stanzas.get(i);
                assertStanza(stanza, BODY + (DiscussionHistory.DEFAULT_HISTORY_SIZE - expectedSize + 2 + i), null);
            }

            // then check subject message
            assertStanza(stanzas.get(expectedSize - 1), null, SUBJECT);
        }
    }

    private void assertStanza(Stanza stanza, String expectedBody, String expectedSubject) throws Exception {
        assertNotNull(stanza);
        MessageStanza msgStanza = (MessageStanza) MessageStanza.getWrapper(stanza);

        assertEquals(new EntityImpl(ROOM_JID, NICK), msgStanza.getFrom());
        assertEquals(RECEIVER, msgStanza.getTo());
        assertEquals("groupchat", msgStanza.getType());
        assertEquals(expectedBody, msgStanza.getBody(null));
        assertEquals(expectedSubject, msgStanza.getSubject(null));
        assertEquals(1, msgStanza.getInnerElementsNamed("delay").size());
    }
}
