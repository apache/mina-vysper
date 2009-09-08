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

import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.TestUtil;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Renderer;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DiscussionHistoryTestCase extends TestCase {

    private static final String NICK = "nick";
    private static final String BODY = "Body";
    private static final String SUBJECT = "Subject";
    
    private static final Entity FROM = TestUtil.parseUnchecked("user@vysper.org/res");
    private static final Entity RECEIVER = TestUtil.parseUnchecked("user2@vysper.org/res");
    private static final Entity ROOM_JID = TestUtil.parseUnchecked("room@vysper.org");

    private static final Occupant FROM_OCCUPANT = new Occupant(FROM, NICK, Affiliation.None, Role.Visitor);
    private static final Occupant RECEIVER_OCCUPANT = new Occupant(RECEIVER, "nick2", Affiliation.None, Role.Visitor);
    private DiscussionHistory history;

    
    @Override
    protected void setUp() throws Exception {
        history = new DiscussionHistory();
        
        // add some messages to the history, one more than is handled
        for(int i = 0; i<DiscussionHistory.DEFAULT_HISTORY_SIZE + 1; i++) {
            history.append(
                    StanzaBuilder.createMessageStanza(FROM, ROOM_JID, MessageStanzaType.GROUPCHAT, null, BODY).getFinalStanza(),
                    FROM_OCCUPANT);
        }
        
        // add a subject message
        history.append(
                StanzaBuilder.createMessageStanza(FROM, ROOM_JID, MessageStanzaType.GROUPCHAT, null, null).
                startInnerElement("subject").addText(SUBJECT).endInnerElement().getFinalStanza(),
                FROM_OCCUPANT);
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

    
    private void assertStanzas(List<Stanza> stanzas, int expectedSize) throws Exception {
        assertEquals(expectedSize, stanzas.size());

        if(expectedSize > 0) {
            
            for(int i = 1; i<expectedSize - 1; i++) {
                Stanza stanza = stanzas.get(i);
                assertStanza(stanza, BODY, null);
            }

            // first check subject message
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
