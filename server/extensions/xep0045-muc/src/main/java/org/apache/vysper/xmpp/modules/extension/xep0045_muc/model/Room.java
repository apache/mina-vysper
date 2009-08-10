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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;


/**
 * A chat room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Room implements InfoRequestListener, ItemRequestListener {

    private EnumSet<RoomType> roomTypes;

    private Entity jid;
    private String name;
    private String password;
    
    // keep in a map to allow for quick access
    private Map<Entity, Occupant> occupants = new ConcurrentHashMap<Entity, Occupant>();
    
    public Room(Entity jid, String name, RoomType... types) {
        if(jid == null) {
            throw new IllegalArgumentException("JID can not be null");
        } else if(jid.getResource() != null) {
            throw new IllegalArgumentException("JID must be bare");
        }
        if(name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Name can not be null or empty");
        }
        
        this.jid = jid;
        this.name = name;
        
        EnumSet<RoomType> potentialTypes;
        if(types != null && types.length > 0) {
            potentialTypes = EnumSet.copyOf(Arrays.asList(types));

            // make sure the list does not contain antonyms
            RoomType.validateAntonyms(potentialTypes);
        } else {
            potentialTypes = EnumSet.noneOf(RoomType.class);
        }
        
        // complement with default types
        this.roomTypes = RoomType.complement(potentialTypes);            
    }

    public Entity getJID() {
        return jid;
    }
    
    public String getName() {
        return name;
    }
    
    public EnumSet<RoomType> getRoomTypes() {
        return roomTypes.clone();
    }
    
    public boolean isRoomType(RoomType type) {
        return roomTypes.contains(type);
    }

    public Occupant addOccupant(Entity occupantJid, String name) {
        // TODO uses a default Affiliation.None, later to be looked up based on the user
        Affiliation affiliation = Affiliation.None;
        Role role = Role.getRole(affiliation, roomTypes);
        Occupant occupant = new Occupant(occupantJid, name, affiliation, role); 
        occupants.put(occupantJid, occupant);
        return occupant;
    }

    
    
    public void removeOccupant(Entity occupantJid) {
        occupants.remove(occupantJid);
    }
    
    public Set<Occupant> getOccupants() {
        Set<Occupant> set = new HashSet<Occupant>();
        for(Occupant occupant : occupants.values()) {
            set.add(occupant);
        }
        
        return Collections.unmodifiableSet(set);
    }
    
    public List<InfoElement> getInfosFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));
        
        for(RoomType type : roomTypes) {
            infoElements.add(new Feature(type.getDiscoName()));            
        }
        
        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
}
