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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;


/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InMemoryRoomStorageProvider implements RoomStorageProvider {

    private Map<Entity, Room> rooms = new ConcurrentHashMap<Entity, Room>();
    
    public void initialize() {
        // do nothing
    }

    public Room createRoom(Entity jid, String name) {
        Room room = new Room(jid, name);
        rooms.put(jid, room);
        return room;
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public Room findRoom(Entity jid) {
        return rooms.get(jid);
    }

    public boolean roomExists(Entity jid) {
        return rooms.containsKey(jid);
    }



    
}
