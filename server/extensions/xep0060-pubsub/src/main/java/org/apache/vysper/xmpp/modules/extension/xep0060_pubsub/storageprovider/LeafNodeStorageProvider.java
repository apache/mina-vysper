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

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.ItemVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.MemberAffiliationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubAffiliation;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LastOwnerResignedException;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;

/**
 * This interface defines all methods a StorageProvider has to offer to be suitable
 * for a LeafNode in the pubsub-module.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public interface LeafNodeStorageProvider extends StorageProvider {

    /**
     * Add a subscriber to the LeafNode.
     * @param nodeName the node JID to which the subscriber should be added.
     * @param subscriptionID the ID for the subscription (multiple subscription ber subscriber).
     * @param subscriber the JID of the subscriber.
     */
    public void addSubscriber(String nodeName, String subscriptionID, Entity subscriber);

    /**
     * Checks if the node specified by nodeName has a subscriber with the given JID.
     * @param nodeName the node to check.
     * @param subscriber the JID of the subscriber.
     * @return true if this JID is subscribed to the node.
     */
    public boolean containsSubscriber(String nodeName, Entity subscriber);

    /**
     * Checks if the node specified by nodeName has a subscriber with the given subscription ID.
     * @param nodeName the node to check.
     * @param subscriptionId the subscription ID to check.
     * @return true if there is a subscription with the given subscription ID.
     */
    public boolean containsSubscriber(String nodeName, String subscriptionId);

    /**
     * Fetches the subscriber for a given subscription ID.
     * @param nodeName the JID of the node.
     * @param subscriptionId the subscription ID we search for.
     * @return the JID of the subscriber with this subscription ID
     */
    public Entity getSubscriber(String nodeName, String subscriptionId);

    /**
     * Removes a subscription based on the subscription ID.
     * @param nodeName the node from which we remove the subscription.
     * @param subscriptionId the ID of the subscription to remove.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean removeSubscription(String nodeName, String subscriptionId);

    /**
     * Removes a subscription based on the JID of the subscriber.
     * @param nodeName the node from which we remove the subscription.
     * @param subscriber the JID to remove.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean removeSubscriber(String nodeName, Entity subscriber);

    /**
     * Count how many subscription a given JID has to a node
     * @param nodeName the node to check
     * @param subscriber the subscriber JID to check
     * @return the number of subscriptions.
     */
    public int countSubscriptions(String nodeName, Entity subscriber);

    /**
     * Count how many subscriptions a given node has.
     * @param nodeName the JID of the node to check.
     * @return the number of subscriptions (should return the number of subscription IDs, not subscribed JIDs).
     */
    public int countSubscriptions(String nodeName);

    /**
     * Store a published message to a node.
     * @param publisher who sent the message
     * @param nodeName the node to which we want to store the message.
     * @param messageID the message ID for later retrieval.
     * @param item the payload of the message.
     */
    public void addMessage(Entity publisher, String nodeName, String messageID, XMLElement item);

    /**
     * Call the SubscriberVisitor for each subscription of the given node.
     * @param nodeName the node we want to iterate.
     * @param subscriberVisitor the SubscriberVisitor to call
     */
    public void acceptForEachSubscriber(String nodeName, SubscriberVisitor subscriberVisitor);

    /**
     * Call to do some preliminary tasks after the module has been configured.
     */
    public void initialize();

    /**
     * Visits each item ever published to the node.
     *
     * @param nodeName
     * @param iv the Visitor.
     */
    public void acceptForEachItem(String nodeName, ItemVisitor iv);

    /**
     * When a new LeafNode is created, initialize will be called with it as a parameter.
     * @param leafNode
     */
    public void initialize(LeafNode leafNode);

    /**
     * Remove the specified node from the storage.
     */
    public void delete(String name);

    /**
     * Add the entity to the owner list of the given node.
     * @param owner
     */
    public void setAffiliation(String nodeName, Entity owner, PubSubAffiliation affiliation)
            throws LastOwnerResignedException;

    /**
     * Returns the affiliation of the entity to the node identified by nodeName.
     */
    public PubSubAffiliation getAffiliation(String nodeName, Entity entity);

    /**
     * Visits each member-affiliation of the given node.
     */
    void acceptForEachMemberAffiliation(String name, MemberAffiliationVisitor mav);
}
