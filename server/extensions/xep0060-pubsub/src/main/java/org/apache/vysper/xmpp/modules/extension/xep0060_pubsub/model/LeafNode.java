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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.ItemVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.MemberAffiliationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubAffiliation;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberPayloadNotificationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.feature.PubsubFeatures;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;

/**
 * This class is the model for leaf nodes. Leaf nodes contain messages and
 * subscribers in various forms.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class LeafNode {

    // the name of the node (free text)
    protected String title;

    // the unique name (per server) of the node
    protected String name;

    // the storage provider for storing and retrieving node information.
    protected LeafNodeStorageProvider storage = null;

    // the service configuration
    protected PubSubServiceConfiguration serviceConfiguration = null;

    /**
     * Creates a new LeafNode with the specified name and title. The creator will be
     * added as owner.
     */
    public LeafNode(PubSubServiceConfiguration serviceConfiguration, String name, String title, Entity creator) {
        init(serviceConfiguration, name, title, creator);
    }

    /**
     * Creates a new LeafNode with the specified name. The creator will be added as
     * owner.
     */
    public LeafNode(PubSubServiceConfiguration serviceConfiguration, String name, Entity creator) {
        init(serviceConfiguration, name, null, creator);
    }

    /**
     * Method to actually do the initialization process.
     * 
     * @param serviceConfiguration
     * @param name
     * @param title
     * @param creator
     */
    private void init(PubSubServiceConfiguration serviceConfiguration, String name, String title, Entity creator) {
        this.serviceConfiguration = serviceConfiguration;
        this.name = name;
        this.title = title;
        this.storage = serviceConfiguration.getLeafNodeStorageProvider();
        this.storage.initialize(this);
        try {
            this.setAffiliation(creator, PubSubAffiliation.OWNER);
        } catch (LastOwnerResignedException e) {
            // won't happen
        }
    }

    /**
     * Changes the persistenceManager.
     * 
     * @param persistenceManager
     *            the new persistence manager.
     */
    public void setPersistenceManager(LeafNodeStorageProvider persistenceManager) {
        this.storage = persistenceManager;
    }

    /**
     * Add a new subscriber with the given id.
     * 
     * @param id
     *            subscription ID
     * @param subscriber
     *            the subscriber
     */
    public void subscribe(String id, Entity subscriber) {
        storage.addSubscriber(name, id, subscriber);
    }

    /**
     * Check whether a JID is already subscribed
     * 
     * @param subscriber
     *            the JID to check
     * @return true if the JID is already subscribed
     */
    public boolean isSubscribed(Entity subscriber) {
        return storage.containsSubscriber(name, subscriber);
    }

    /**
     * Check whether if we already have a subscription with the given ID
     * 
     * @param subscriptionID
     *            the ID to check for.
     * @return true if a subscription with this ID is present.
     */

    public boolean isSubscribed(String subscriptionID) {
        return storage.containsSubscriber(name, subscriptionID);
    }

    /**
     * Remove a subscription of a JID with a given subscription ID.
     * 
     * @param subscriptionID
     *            the subscription ID of the JID.
     * @param subscriber
     *            the JID of the subscriber.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean unsubscribe(String subscriptionID, Entity subscriber) {
        Entity sub = storage.getSubscriber(name, subscriptionID);

        if (sub != null && sub.equals(subscriber)) {
            return storage.removeSubscription(name, subscriptionID);
        }
        return false;
    }

    /**
     * Remove a subscription solely with the subscription JID, if more than one
     * subscription with this JID is present an exception will be thrown.
     * 
     * @param subscriber
     *            the JID to unsubscribe.
     * @return true if the subscription has been removed.
     * @throws MultipleSubscriptionException
     *             if more than one subscription with this JID is present.
     */
    public boolean unsubscribe(Entity subscriber) throws MultipleSubscriptionException {
        if (countSubscriptions(subscriber) > 1) {
            throw new MultipleSubscriptionException("Ambigous unsubscription request");
        }
        return storage.removeSubscriber(name, subscriber);
    }

    /**
     * Returns the number of subscriptions with the given JID.
     * 
     * @param subscriber
     *            the JID to count.
     * @return number of subscriptions.
     */
    public int countSubscriptions(Entity subscriber) {
        return storage.countSubscriptions(name, subscriber);
    }

    /**
     * Returns the total number of subscriptions (based on the subscriptions IDs,
     * not the JIDs).
     * 
     * @return number of subscriptions.
     */
    public int countSubscriptions() {
        return storage.countSubscriptions(name);
    }

    /**
     * Publish an item to this node.
     * 
     * @param sender
     *            the sender of the message (publisher).
     * @param broker
     *            the relay for sending the messages.
     * @param itemID
     *            the ID of the published message.
     * @param item
     *            the payload of the message.
     */
    public void publish(Entity sender, StanzaBroker broker, String itemID, XMLElement item) {
        storage.addMessage(sender, name, itemID, item);
        sendMessageToSubscriber(broker, item);
    }

    /**
     * Sends a message to each subscriber of the node.
     * 
     * @param stanzaBroker
     *            the relay for sending the notifications.
     * @param item
     *            the payload of the message.
     */
    protected void sendMessageToSubscriber(StanzaBroker stanzaBroker, XMLElement item) {
        storage.acceptForEachSubscriber(name,
                new SubscriberPayloadNotificationVisitor(serviceConfiguration.getDomainJID(), stanzaBroker, item));
    }

    /**
     * Call the SubscriberVisitor for each subscription of this node.
     * 
     * @param sv
     */
    public void acceptSubscribers(SubscriberVisitor sv) {
        storage.acceptForEachSubscriber(name, sv);
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
     * Builds a list of InfoElements for disco#info requests.
     * 
     * @param request
     *            the sent request
     * @return the list of InfoElements
     */
    public List<InfoElement> getNodeInfosFor(InfoRequest request) {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("pubsub", "leaf"));
        infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB));
        infoElements.add(PubsubFeatures.ACCESS_OPEN.getFeature());
        infoElements.add(PubsubFeatures.ITEM_IDS.getFeature());
        infoElements.add(PubsubFeatures.PERSISTENT_ITEMS.getFeature());
        infoElements.add(PubsubFeatures.MULTI_SUBSCRIBE.getFeature());
        infoElements.add(PubsubFeatures.PUBLISH.getFeature());
        infoElements.add(PubsubFeatures.SUBSCRIBE.getFeature());
        infoElements.add(PubsubFeatures.RETRIEVE_SUBSCRIPTIONS.getFeature());
        infoElements.add(PubsubFeatures.RETRIEVE_AFFILIATIONS.getFeature());
        infoElements.add(PubsubFeatures.MODIFY_AFFILIATIONS.getFeature());
        return infoElements;
    }

    /**
     * Visits each item ever published to this node.
     * 
     * @param iv
     *            the visitor
     */
    public void acceptItems(ItemVisitor iv) {
        storage.acceptForEachItem(name, iv);
    }

    /**
     * Visits each member of this node.
     *
     * @param mav
     *            the visitor
     */
    public void acceptMemberAffiliations(MemberAffiliationVisitor mav) {
        storage.acceptForEachMemberAffiliation(name, mav);
    }

    /**
     * Called after all information, including the persistencemanager for the node
     * is set.
     */
    public void initialize() {
        storage.initialize(this);
    }

    /**
     * Removes this node from the storage.
     */
    public void delete() {
        this.storage.delete(this.name);
    }

    /**
     * Adds the given entity to the owner list.
     * 
     * @param owner
     */
    public void setAffiliation(Entity owner, PubSubAffiliation affiliation) throws LastOwnerResignedException {
        this.storage.setAffiliation(name, owner, affiliation);
    }

    /**
     * Check whether the given JID is allowed to perform the requested task.
     * 
     * @param sender
     * @param requestedAffiliation
     * @return
     */
    public boolean isAuthorized(Entity sender, PubSubAffiliation requestedAffiliation) {
        PubSubAffiliation affiliation = this.storage.getAffiliation(name, sender);
        return affiliation.compareTo(requestedAffiliation) >= 0;
    }

    /**
     * Returns the affiliation for the given bareJID.
     * 
     * @param entity
     *            the entity for which the affiliation should be returned.
     * @return All affiliations ("NONE" if no other affiliation is known).
     */
    public PubSubAffiliation getAffiliation(Entity entity) {
        return this.storage.getAffiliation(name, entity);
    }
}
