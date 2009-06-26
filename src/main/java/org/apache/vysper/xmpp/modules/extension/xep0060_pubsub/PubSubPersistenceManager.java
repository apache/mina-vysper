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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * This interface defines all methods a StorageProvider has to offer to be suitable
 * for the pubsub-module.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public interface PubSubPersistenceManager extends StorageProvider {

    // CollectionNode methods
    
    /**
     * Retrieve a node via its JID
     * @param jid the JID of the node we're searching for
     * @return the LeafNode if found, null otherwise.
     */
    public LeafNode findNode(Entity jid);

    /**
     * Checks whether the collection node contains a node with a certain JID.
     * @param jid the JID we're checking for.
     * @return true if the JID corresponds to a known node.
     */
    public boolean containsNode(Entity jid);

    /**
     * Stores a node under the specified JID.
     * @param jid the JID for the node.
     * @param node the LeafNode to be stored.
     */
    public void storeNode(Entity jid, LeafNode node);

    // LeafNode methods (subscriptions)
    /**
     * Add a subscriber to the LeafNode.
     * @param nodeJID the node JID to which the subscriber should be added.
     * @param subscriptionID the ID for the subscription (multiple subscription ber subscriber).
     * @param subscriber the JID of the subscriber.
     */
    public void addSubscriber(Entity nodeJID, String subscriptionID, Entity subscriber);

    /**
     * Checks if the node specified by nodeJID has a subscriber with the given JID.
     * @param nodeJID the node to check.
     * @param subscriber the JID of the subscriber.
     * @return true if this JID is subscribed to the node.
     */
    public boolean containsSubscriber(Entity nodeJID, Entity subscriber);

    /**
     * Checks if the node specified by nodeJID has a subscriber with the given subscription ID.
     * @param nodeJID the node to check.
     * @param subscriptionId the subscription ID to check.
     * @return true if there is a subscription with the given subscription ID.
     */
    public boolean containsSubscriber(Entity nodeJID, String subscriptionId);

    /**
     * Fetches the subscriber for a given subscription ID.
     * @param nodeJID the JID of the node.
     * @param subscriptionId the subscription ID we search for.
     * @return the JID of the subscriber with this subscription ID
     */
    public Entity getSubscriber(Entity nodeJID, String subscriptionId);

    /**
     * Removes a subscription based on the subscription ID.
     * @param nodeJID the node from which we remove the subscription.
     * @param subscriptionId the ID of the subscription to remove.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean removeSubscription(Entity nodeJID, String subscriptionId);

    /**
     * Removes a subscription based on the JID of the subscriber.
     * @param nodeJID the node from which we remove the subscription.
     * @param subscriber the JID to remove.
     * @return true if the subscription has been removed, false otherwise.
     */
    public boolean removeSubscriber(Entity nodeJID, Entity subscriber);

    /**
     * Count how many subscription a given JID has to a node
     * @param nodeJID the node to check
     * @param subscriber the subscriber JID to check
     * @return the number of subscriptions.
     */
    public int countSubscriptions(Entity nodeJID, Entity subscriber);

    /**
     * Count how many subscriptions a given node has.
     * @param nodeJID the JID of the node to check.
     * @return the number of subscriptions (should return the number of subscription IDs, not subscribed JIDs).
     */
    public int countSubscriptions(Entity nodeJID);

    /**
     * Store a published message to a node.
     * @param nodeJID the node to which we want to store the message.
     * @param messageID the message ID for later retrieval.
     * @param item the payload of the message.
     */
    public void addMessage(Entity nodeJID, String messageID, XMLElement item);

    /**
     * Call the SubscriberVisitor for each subscription of the given node.
     * @param nodeJID the node we want to iterate.
     * @param subscriberVisitor the SubscriberVisitor to call
     */
    public void acceptForEachSubscriber(Entity nodeJID, SubscriberVisitor subscriberVisitor);

}
