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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.inttest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.TestUtil;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.AbstractIntegrationTestCase;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 */
public class MUCIntegrationTestCase extends AbstractIntegrationTestCase {

    private static final String MUC_SUBDOMAIN = "chat";
    private static final String ROOM_JID = "room@chat.vysper.org";
    
    private Conference conference = new Conference("test conference");
    
    
    
    private XMPPConnection client2;
    private MultiUserChat chat;
    private MultiUserChat chat2;
    
    @Override
    protected void addModules(XMPPServer server) {
        server.addModule(new MUCModule(MUC_SUBDOMAIN, conference));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        client2 = connectClient(port, TEST_USERNAME2, TEST_PASSWORD2);

        chat = new MultiUserChat(client, ROOM_JID);
        chat2 = new MultiUserChat(client2, ROOM_JID);
    }

    public void testEnterRoom() throws Exception {
        chat.join("Nick");

        Room room = conference.findRoom(TestUtil.parseUnchecked(ROOM_JID));
        assertEquals(1, room.getOccupantCount());
        Occupant occupant = room.getOccupants().iterator().next();
        assertEquals(TEST_USERNAME1, occupant.getJid().getBareJID().getFullQualifiedName());
        assertEquals("Nick", occupant.getName());
        
        
        final BlockingQueue<String> joinedQueue = new LinkedBlockingQueue<String>();
        chat.addParticipantStatusListener(new ParticipantStatusListenerAdapter() {
            
            @Override
            public void joined(String participant) {
                joinedQueue.add(participant);
            }
        });
        chat2.join("Nick2");
        assertEquals(2, room.getOccupantCount());

        // chat should be notified
        assertEquals(ROOM_JID + "/Nick2", joinedQueue.poll(5000, TimeUnit.MILLISECONDS));
    }
    
    public void testExitRoom() throws Exception {
        chat.join("Nick");
        chat2.join("Nick2");

        Room room = conference.findRoom(TestUtil.parseUnchecked(ROOM_JID));
        assertEquals(2, room.getOccupantCount());
        
        final BlockingQueue<String> leftQueue = new LinkedBlockingQueue<String>();
        chat.addParticipantStatusListener(new ParticipantStatusListenerAdapter() {
            @Override
            public void left(String participant) {
                leftQueue.add(participant);
            }
        });

        
        chat2.leave();
        
        // wait for status update
        assertEquals(ROOM_JID + "/Nick2", leftQueue.poll(5000, TimeUnit.MILLISECONDS));
        assertEquals(1, room.getOccupantCount());

    }

    public void testSendMessageToRoom() throws Exception {
        chat.join("Nick");
        chat2.join("Nick2");

        chat.sendMessage("Fooo");
        Message message = chat.nextMessage(5000);

        assertNotNull(message);
        assertEquals("Fooo", message.getBody());
        assertEquals(ROOM_JID + "/Nick", message.getFrom());
        assertEquals(TEST_USERNAME1, EntityImpl.parse(message.getTo()).getBareJID().getFullQualifiedName());
        
        message = chat2.nextMessage(5000);
        assertNotNull(message);
        assertEquals("Fooo", message.getBody());
        assertEquals(ROOM_JID + "/Nick", message.getFrom());
        assertEquals(TEST_USERNAME2, EntityImpl.parse(message.getTo()).getBareJID().getFullQualifiedName());
    }
}
