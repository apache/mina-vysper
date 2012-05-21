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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCFeatures;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ConferenceTestCase extends TestCase {

    private Entity jid1 = EntityImpl.parseUnchecked("jid1@vysper.org");

    private Entity jid2 = EntityImpl.parseUnchecked("jid2@vysper.org");

    public void testGetName() {
        final MUCFeatures mucFeatures = new MUCFeatures();
        mucFeatures.setMaxRoomHistoryItems(20);
        Conference conference = new Conference("foo", mucFeatures);
        assertEquals("foo", conference.getName());
    }

    public void testConstructNullName() {
        try {
            new Conference(null, new MUCFeatures());
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructEmptyName() {
        try {
            new Conference("", new MUCFeatures());
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructWhitespaceName() {
        try {
            new Conference("\t ", new MUCFeatures());
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateGetRooms() {
        Conference conference = new Conference("foo", new MUCFeatures());
        conference.createRoom(jid1, "room1");
        conference.createRoom(jid2, "room2");

        Collection<Room> rooms = conference.getAllRooms();
        List<String> roomNames = new ArrayList<String>();
        for (Room room : rooms) {
            roomNames.add(room.getName());
        }

        assertTrue(roomNames.contains("room1"));
        assertTrue(roomNames.contains("room2"));
    }

    public void testCreateDuplicateRooms() throws Exception {
        Conference conference = new Conference("foo", new MUCFeatures());
        conference.createRoom(jid1, "room1");
        try {
            // make sure we use a different JID instance
            Entity duplicateJID = EntityImpl.parse("jid1@vysper.org");

            conference.createRoom(duplicateJID, "room1");
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
