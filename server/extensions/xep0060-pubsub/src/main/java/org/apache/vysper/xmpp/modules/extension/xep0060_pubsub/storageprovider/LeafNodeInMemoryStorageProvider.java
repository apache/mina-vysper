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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.ItemVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.MemberAffiliationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubAffiliation;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LastOwnerResignedException;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.PayloadItem;

/**
 * This storage provider keeps all objects in memory and looses its content when
 * removed from memory. This is the default storage provider for leaf nodes.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class LeafNodeInMemoryStorageProvider implements LeafNodeStorageProvider {

    // The node owners
    protected Map<String, Map<Entity, PubSubAffiliation>> nodeAffiliations;

    // stores subscribers to a node, access via subid
    protected Map<String, Map<String, Entity>> nodeSubscribers;

    // stores messages to a node, access via itemid
    protected Map<String, Map<String, PayloadItem>> nodeMessages;

    /**
     * Initialize the storage maps.
     */
    public LeafNodeInMemoryStorageProvider() {
        this.nodeSubscribers = new TreeMap<String, Map<String, Entity>>();
        this.nodeMessages = new TreeMap<String, Map<String, PayloadItem>>();
        this.nodeAffiliations = new TreeMap<String, Map<Entity, PubSubAffiliation>>();
    }

    /**
     * Add a subscriber with given subID.
     */
    public void addSubscriber(String nodeName, String subscriptionID, Entity subscriber) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        subscribers.put(subscriptionID, subscriber);
    }

    /**
     * Check if a subscriber is already known.
     */
    public boolean containsSubscriber(String nodeName, Entity subscriber) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.containsValue(subscriber);
    }

    /**
     * Check if a subscriptionId is already known.
     */
    public boolean containsSubscriber(String nodeName, String subscriptionId) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.containsKey(subscriptionId);
    }

    /**
     * Retrieve a subscriber via its subsriptionId.
     */
    public Entity getSubscriber(String nodeName, String subscriptionId) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.get(subscriptionId);
    }

    /**
     * Remove a subscriber via its subscriptionId.
     */
    public boolean removeSubscription(String nodeName, String subscriptionId) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.remove(subscriptionId) != null;
    }

    /**
     * Remove a subscriber via its JID. This removes all subscriptions of the JID.
     */
    public boolean removeSubscriber(String nodeName, Entity subscriber) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.values().remove(subscriber);
    }

    /**
     * Count how often a given subscriber is subscribed.
     */
    public int countSubscriptions(String nodeName, Entity subscriber) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        int count = 0;
        for (Entity sub : subscribers.values()) {
            if (subscriber.equals(sub)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Count how many subscriptions this node has.
     */
    public int countSubscriptions(String nodeName) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        return subscribers.size();
    }

    /**
     * Add a message to the storage.
     */
    public void addMessage(Entity publisher, String nodeName, String itemID, XMLElement payload) {
        Map<String, PayloadItem> messages = nodeMessages.get(nodeName);
        messages.put(itemID, new PayloadItem(publisher, payload, itemID));
    }

    /**
     * Accept method (see visitor pattern) to visit all subscribers of this node.
     */
    public void acceptForEachSubscriber(String nodeName, SubscriberVisitor subscriberVisitor) {
        Map<String, Entity> subscribers = nodeSubscribers.get(nodeName);
        for (String subID : subscribers.keySet()) {
            subscriberVisitor.visit(nodeName, subID, subscribers.get(subID));
        }
    }

    /**
     * The in-memory storage provider does not need initialization beyond creating the objects.
     */
    public void initialize() {
        // empty
    }

    /**
     * Go through each message and call visit of the visitor.
     */
    public void acceptForEachItem(String nodeName, ItemVisitor iv) {
        Map<String, PayloadItem> messages = nodeMessages.get(nodeName);
        for (String itemID : messages.keySet()) {
            iv.visit(itemID, messages.get(itemID));
        }
    }

    /**
     * Initialize the node with the storage.
     */
    public void initialize(LeafNode leafNode) {
        nodeMessages.put(leafNode.getName(), new TreeMap<String, PayloadItem>());
        nodeSubscribers.put(leafNode.getName(), new TreeMap<String, Entity>());
        nodeAffiliations.put(leafNode.getName(), new HashMap<Entity, PubSubAffiliation>());
    }

    /**
     * Remove the specified node from the storage.
     */
    public void delete(String name) {
        nodeMessages.remove(name);
        nodeSubscribers.remove(name);
        nodeAffiliations.remove(name);
    }

    /**
     * Add the entity to the owner list of the given node.
     * The owner is stored as bare JID.
     */
    public void setAffiliation(String nodeName, Entity entity, PubSubAffiliation affiliation)
            throws LastOwnerResignedException {
        Map<Entity, PubSubAffiliation> affils = this.nodeAffiliations.get(nodeName);
        Entity bareJID = entity.getBareJID();

        if (getAffiliation(nodeName, bareJID).equals(PubSubAffiliation.OWNER)
                && !affiliation.equals(PubSubAffiliation.OWNER)
                && countAffiliations(nodeName, PubSubAffiliation.OWNER) == 1) {
            throw new LastOwnerResignedException(bareJID.getFullQualifiedName() + " tried to resign from " + nodeName);
        }

        if (affiliation.equals(PubSubAffiliation.NONE)) {
            affils.remove(bareJID); // NONE affiliations are not stored.
        } else {
            affils.put(bareJID, affiliation);
        }
    }

    /**
     * Calculates how many users with the given affiliation are present for this node.
     * @param nodeName the node to check
     * @param affiliation to count
     * @return the number of owners.
     */
    private int countAffiliations(String nodeName, PubSubAffiliation affiliation) {
        Map<Entity, PubSubAffiliation> affils = this.nodeAffiliations.get(nodeName);
        int i = 0;
        for (PubSubAffiliation a : affils.values()) {
            if (a.equals(affiliation))
                ++i;
        }
        return i;
    }

    /**
     * Returns the affiliation of the entity to the node. Only the bare JID will be compared.
     */
    public PubSubAffiliation getAffiliation(String nodeName, Entity entity) {
        PubSubAffiliation psa = this.nodeAffiliations.get(nodeName).get(entity.getBareJID());
        return psa != null ? psa : PubSubAffiliation.NONE; // NONE if there is no affiliation known.
    }

    /**
     * Call the visitor with the each member JID and its associated affiliation.
     */
    public void acceptForEachMemberAffiliation(String name, MemberAffiliationVisitor mav) {
        Map<Entity, PubSubAffiliation> affils = this.nodeAffiliations.get(name);
        for (Entity jid : affils.keySet()) {
            PubSubAffiliation affil = affils.get(jid);
            mav.visit(jid, affil);
        }
    }
}
