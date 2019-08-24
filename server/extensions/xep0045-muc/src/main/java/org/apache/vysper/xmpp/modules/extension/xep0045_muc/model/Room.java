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
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.PresenceStanza;

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
    
    private boolean rewriteDuplicateNick = true;
    
    private boolean visitorsHaveVoice = false;

    private DiscussionHistory history = new DiscussionHistory();

    private Affiliations affiliations = new Affiliations();

    // keep in a map to allow for quick access
    private Map<Entity, Occupant> occupants = new ConcurrentHashMap<Entity, Occupant>();
    
    private Map<Entity, PresenceStanza> occupantsLatestPresence = new ConcurrentHashMap<Entity, PresenceStanza>();
    
    protected OccupantStorageProvider occupantStorageProvider;
    
    protected long createdTimestamp;
    
    protected long lastActivity = -1;

    public Room(Entity jid, String name, RoomType... types) {
        if (jid == null) {
            throw new IllegalArgumentException("JID can not be null");
        } else if (jid.getResource() != null) {
            throw new IllegalArgumentException("JID must be bare");
        }
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Name can not be null or empty");
        }

        this.jid = jid;
        this.name = name;
        this.createdTimestamp = System.currentTimeMillis();

        EnumSet<RoomType> potentialTypes;
        if (types != null && types.length > 0) {
            potentialTypes = EnumSet.copyOf(Arrays.asList(types));

            // make sure the list does not contain antonyms
            RoomType.validateAntonyms(potentialTypes);
        } else {
            potentialTypes = EnumSet.noneOf(RoomType.class);
        }

        // complement with default types
        this.roomTypes = RoomType.complement(potentialTypes);
    }

    public void setOccupantStorageProvider(OccupantStorageProvider occupantStorageProvider) {
        this.occupantStorageProvider = occupantStorageProvider;
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

    public boolean rewritesDuplicateNick() {
        return rewriteDuplicateNick;
    }

    public void setRewriteDuplicateNick(boolean rewriteDuplicateNick) {
        this.rewriteDuplicateNick = rewriteDuplicateNick;
    }

    public void setMaxRoomHistoryItems(int maxItems) {
        history.setMaxItems(maxItems);
    }

    public boolean doVisitorsHaveVoice() {
        return visitorsHaveVoice;
    }

    public void setVisitorsHaveVoice(boolean visitorsHaveVoice) {
        this.visitorsHaveVoice = visitorsHaveVoice;
    }

    public Occupant addOccupant(Entity occupantJid, String name) {
        Affiliation affiliation = affiliations.getAffiliation(occupantJid);

        // TODO throw a domain specific exception
        if (affiliation == Affiliation.Outcast) {
            throw new RuntimeException("forbidden");
        }

        // default to none
        if (affiliation == null) {
            affiliation = Affiliation.None;
        }
        
        Role role = Role.getRole(affiliation, roomTypes);
        Occupant occupant = new Occupant(occupantJid, name, this, role);
        if (isRoomType(RoomType.MembersOnly) && affiliation == Affiliation.None) {
            // don't add non member to room
            throw new RuntimeException("registration-required");
        } else {
            occupants.put(occupantJid, occupant);
        }
        if (occupantStorageProvider != null) occupantStorageProvider.occupantAdded(this, occupant);
        return occupant;
    }

    public Occupant findOccupantByJID(Entity occupantJid) {
        return occupants.get(occupantJid);
    }

    public Occupant findOccupantByNick(String nick) {
        for (Occupant occupant : getOccupants()) {
            if (occupant.getNick().equals(nick)) return occupant;
        }

        return null;
    }

    public void recordLatestPresence(Entity occupantJid, PresenceStanza presenceStanza) {
        if (!isInRoom(occupantJid)) return;
        occupantsLatestPresence.put(occupantJid, presenceStanza);
    }

    public PresenceStanza getLatestPresence(Entity occupantJid) {
        final PresenceStanza lastPresence = occupantsLatestPresence.get(occupantJid);
        if (lastPresence == null || !isInRoom(occupantJid)) return null;
        return lastPresence;
    }
    
    public Set<Occupant> getModerators() {
        return getByRole(Role.Moderator);
    }
    
    private Set<Occupant> getByRole(Role role) {
        Set<Occupant> matches = new HashSet<Occupant>();
        for (Occupant occupant : getOccupants()) {
            if (role.equals(occupant.getRole())) matches.add(occupant);
        }
        return matches;
    }
    
    public boolean isInRoom(Entity jid) {
        return findOccupantByJID(jid) != null;
    }

    public boolean isInRoom(String nick) {
        return findOccupantByNick(nick) != null;
    }

    public void removeOccupant(Entity occupantJid) {
        final Occupant removed = occupants.remove(occupantJid);
        occupantsLatestPresence.remove(occupantJid);
        if (occupantStorageProvider != null) occupantStorageProvider.occupantRemoved(this, removed);
    }

    public int getOccupantCount() {
        return occupants.size();
    }

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    public Set<Occupant> getOccupants() {
        Set<Occupant> set = new HashSet<Occupant>(occupants.values());
        return Collections.unmodifiableSet(set);
    }

    public List<InfoElement> getInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));

        for (RoomType type : roomTypes) {
            if (type.includeInDisco()) {
                infoElements.add(new Feature(type.getDiscoName()));
            }
        }

        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {

        // TODO is this the right way to determine if the room is private?
        if (isRoomType(RoomType.FullyAnonymous) || isRoomType(RoomType.SemiAnonymous)) {
            // private room, return empty list
            return Collections.emptyList();
        }

        // List of users
        List<Item> items = new ArrayList<Item>();
        
        for (Occupant occupant : getOccupants()) {
            items.add(new Item(new EntityImpl(getJID(), occupant.getNick())));
        }
        return items;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DiscussionHistory getHistory() {
        return history;
    }

    public Affiliations getAffiliations() {
        return affiliations;
    }

    /**
     * @return time stamp in milliseconds when the room has been created
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void updateLastActivity() {
        this.lastActivity =  Math.max(lastActivity, System.currentTimeMillis());
    }

    /**
     * @return time stamp in milliseconds when the last message was sent or a occupant joined/left
     */
    public long getLastActivity() {
        return lastActivity;
    }
}
