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

import java.util.EnumSet;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RoomTestCase extends TestCase {

    private Entity roomJid1 = EntityImpl.parseUnchecked("room1@vysper.org");

    private Entity roomJid2 = EntityImpl.parseUnchecked("room2@vysper.org");

    private Entity occupantJid1 = EntityImpl.parseUnchecked("user1@vysper.org");

    private Entity occupantJid2 = EntityImpl.parseUnchecked("user2@vysper.org");

    public void testConstructor() {
        Room room = new Room(roomJid1, "Room 1");
        assertEquals(roomJid1, room.getJID());
        assertEquals("Room 1", room.getName());

        EnumSet<RoomType> types = room.getRoomTypes();
        assertTrue(types.contains(RoomType.NonAnonymous));
        assertTrue(types.contains(RoomType.Open));
        assertTrue(types.contains(RoomType.Public));
        assertTrue(types.contains(RoomType.Temporary));
        assertTrue(types.contains(RoomType.Unmoderated));
        assertTrue(types.contains(RoomType.Unsecured));
    }

    public void testConstructorWithTypes() {
        Room room = new Room(roomJid1, "Room 1", RoomType.Hidden, RoomType.Open);
        assertEquals(roomJid1, room.getJID());
        assertEquals("Room 1", room.getName());

        EnumSet<RoomType> types = room.getRoomTypes();
        assertTrue(types.contains(RoomType.NonAnonymous));
        assertTrue(types.contains(RoomType.Open));
        // must be hidden
        assertTrue(types.contains(RoomType.Hidden));
        assertTrue(types.contains(RoomType.Temporary));
        assertTrue(types.contains(RoomType.Unmoderated));
        assertTrue(types.contains(RoomType.Unsecured));
    }

    public void testConstructorWithAntonymTypes() {
        try {
            new Room(roomJid1, "Room 1", RoomType.Hidden, RoomType.Public);
            fail("Expects IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructorWithFullJID() {
        try {
            new Room(EntityImpl.parseUnchecked("jid@vysper.org/incorrect"), "Room 1");
            fail("Expects IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructorWithNullJID() {
        try {
            new Room(null, "Room 1");
            fail("Expects IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructorWithNullName() {
        try {
            new Room(roomJid1, null);
            fail("Expects IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testConstructorWithWhitespaceName() {
        try {
            new Room(roomJid1, " \t ");
            fail("Expects IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testAddRemoveGetOccupant() {
        Room room = new Room(roomJid1, "Room 1");
        room.addOccupant(occupantJid1, "Nick 1");

        assertEquals(1, room.getOccupantCount());

        Occupant occupant = room.getOccupants().iterator().next();
        assertEquals(occupantJid1, occupant.getJid());
        assertEquals("Nick 1", occupant.getNick());

        room.addOccupant(occupantJid2, "Nick 2");
        assertEquals(2, room.getOccupantCount());

        room.removeOccupant(occupantJid1);

        assertEquals(1, room.getOccupantCount());

        occupant = room.getOccupants().iterator().next();
        assertEquals(occupantJid2, occupant.getJid());
    }

    public void testFindOccupantByJID() {
        Room room = new Room(roomJid1, "Room 1");
        room.addOccupant(occupantJid1, "Nick 1");
        room.addOccupant(occupantJid2, "Nick 2");

        Occupant occupant = room.findOccupantByJID(occupantJid1);
        assertNotNull(occupant);
        assertEquals(occupantJid1, occupant.getJid());

        assertNull(room.findOccupantByJID(EntityImpl.parseUnchecked("dummy@vysper.org")));
    }

    public void testFindOccupantByNick() {
        Room room = new Room(roomJid1, "Room 1");
        room.addOccupant(occupantJid1, "Nick 1");
        room.addOccupant(occupantJid2, "Nick 2");

        Occupant occupant = room.findOccupantByNick("Nick 2");
        assertNotNull(occupant);
        assertEquals(occupantJid2, occupant.getJid());

        assertNull(room.findOccupantByNick("Dummy"));
    }
}
