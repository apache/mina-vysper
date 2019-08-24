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
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCFeatures;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryOccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryRoomStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;

/**
 * Represents the root of a conference, containing rooms
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Conference implements ServerInfoRequestListener, ItemRequestListener {

    protected String name;

    protected final MUCFeatures mucFeatures;
    
    private RoomStorageProvider roomStorageProvider = new InMemoryRoomStorageProvider();

    private OccupantStorageProvider occupantStorageProvider = new InMemoryOccupantStorageProvider();

    public Conference(String name, MUCFeatures mucFeatures) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        this.name = name;

        this.mucFeatures = mucFeatures;
    }

    public void initialize() {
        if (roomStorageProvider != null) roomStorageProvider.initialize();
        if (occupantStorageProvider != null) occupantStorageProvider.initialize();
    }

    /*package*/ Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(roomStorageProvider.getAllRooms());
    }

    public synchronized Room createRoom(Entity jid, String name, RoomType... types) {
        if (roomStorageProvider.roomExists(jid)) {
            throw new IllegalArgumentException("Room already exists with JID: " + jid);
        }

        final Room room = roomStorageProvider.createRoom(mucFeatures, jid, name, types);
        if (occupantStorageProvider != null) room.setOccupantStorageProvider(occupantStorageProvider);
        return room;
    }

    public void deleteRoom(Entity jid) {
        roomStorageProvider.deleteRoom(jid);
    }

    public Room findRoom(Entity jid) {
        return roomStorageProvider.findRoom(jid);
    }

    public OccupantStorageProvider getOccupantStorageProvider() {
        return occupantStorageProvider;
    }

    public void setOccupantStorageProvider(OccupantStorageProvider occupantStorageProvider) {
        this.occupantStorageProvider = occupantStorageProvider;
    }

    public RoomStorageProvider getRoomStorageProvider() {
        return roomStorageProvider;
    }

    public void setRoomStorageProvider(RoomStorageProvider roomStorageProvider) {
        this.roomStorageProvider = roomStorageProvider;
    }

    public String getName() {
        return name;
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        if (request.getNode() != null && request.getNode().length() > 0) return null;
        
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));
        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker) {
        List<Item> items = new ArrayList<Item>();
        Collection<Room> rooms = getAllRooms();

        for (Room room : rooms) {
            if (room.isRoomType(RoomType.Hidden)) continue;
            items.add(new Item(room.getJID(), room.getName()));
        }

        return items;
    }

}
