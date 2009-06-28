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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberNotificationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * This class is the model for leaf nodes. Leaf nodes contain messages and subscribers in various
 * forms.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class LeafNode {

    // the jid of the server the node is on
    protected Entity serverJID;
    // the name of the node (free text)
    protected String title;
    // the unique name (per server) of the node
    protected String name;
    // the storage provider for storing and retrieving node information.
    protected LeafNodeStorageProvider storage = new LeafNodeInMemoryStorageProvider();

    /**
     * Creates a new LeafNode with the specified JID.
     * @param serverJID the JID of the node
     * @param name the unique name of the node (per server)
     * @param title the free-text title of the node (what is it about?)
     */
    public LeafNode(Entity serverJID, String name, String title) {
        this.serverJID = serverJID;
        this.name = name;
        this.title = title;
    }

    /**
     * Changes the persistenceManager.
     * @param persistenceManager the new persistence manager.
     */
    public void setPersistenceManager(LeafNodeStorageProvider persistenceManager) {
        this.storage = persistenceManager;
    }

    /**
     * Add a new subscriber with the given id.
     * @param id subscription ID
     * @param subscriber the subscriber
     */
    public void subscribe(String id, Entity subscriber) {
        storage.addSubscriber(serverJID, id, subscriber);
    }

    /**
     * Check whether a JID is already subscribed
     * @param subscriber the JID to check
     * @return true if the JID is already subscribed
     */
    public boolean isSubscribed(Entity subscriber) {
        return storage.containsSubscriber(serverJID, subscriber);
    }

    /**
     * Check whether if we already have a subscription with the given ID
     * @param subscriptionID the ID to check for.
     * @return true if a subscription with this ID is present.
     */
    
    public boolean isSubscribed(String subscriptionID) {
        return storage.containsSubscriber(serverJID, subscriptionID);
    }

    /**
     * Remove a subscription of a JID with a given subscription ID.
     * @param subscriptionID the subscription ID of the JID.
     * @param subscriber the JID of the subscriber.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean unsubscribe(String subscriptionID, Entity subscriber) {
        Entity sub = storage.getSubscriber(serverJID, subscriptionID);

        if(sub != null && sub.equals(subscriber)) {
            return storage.removeSubscription(serverJID, subscriptionID);
        }
        return false;
    }

    /**
     * Remove a subscription solely with the subscription JID, if more than one subscription
     * with this JID is present an exception will be thrown.
     * 
     * @param subscriber the JID to unsubscribe.
     * @return true if the subscription has been removed.
     * @throws MultipleSubscriptionException if more than one subscription with this JID is present.
     */
    public boolean unsubscribe(Entity subscriber) throws MultipleSubscriptionException {
        if(countSubscriptions(subscriber) > 1) {
            throw new MultipleSubscriptionException("Ambigous unsubscription request");
        }
        return storage.removeSubscriber(serverJID, subscriber);
    }

    /**
     * Returns the number of subscriptions with the given JID.
     * @param subscriber the JID to count.
     * @return number of subscriptions.
     */
    public int countSubscriptions(Entity subscriber) {
        return storage.countSubscriptions(serverJID, subscriber);
    }

    /**
     * Returns the total number of subscriptions (based on the subscriptions IDs, not the JIDs).
     * @return number of subscriptions.
     */
    public int countSubscriptions() {
        return storage.countSubscriptions(serverJID);
    }

    /**
     * Publish an item to this node.
     * @param sender the sender of the message (publisher).
     * @param relay the relay for sending the messages.
     * @param messageID the ID of the published message.
     * @param item the payload of the message.
     */
    public void publish(Entity sender, StanzaRelay relay, String messageID, XMLElement item) {
        storage.addMessage(serverJID, messageID, item);
        sendMessageToSubscriber(relay, item);
    }

    /**
     * Sends a message to each subscriber of the node.
     * 
     * @param stanzaRelay the relay for sending the notifications.
     * @param item the payload of the message.
     */
    protected void sendMessageToSubscriber(StanzaRelay stanzaRelay, XMLElement item) {
        storage.acceptForEachSubscriber(serverJID, new SubscriberNotificationVisitor(stanzaRelay, item));
    }

    /**
     * @return the name of the node.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the free-text title of the node
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * @return the JID of the server the nodes lies on.
     */
    public Entity getServerJID() {
        return serverJID;
    }

    /**
     * Builds a list of InfoElements for disco#info requests.
     * 
     * @param request the sent request
     * @return the list of InfoElements
     */
    public List<InfoElement> getNodeInfosFor(InfoRequest request) {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("pubsub", "leaf"));
        return infoElements;
    }
}
