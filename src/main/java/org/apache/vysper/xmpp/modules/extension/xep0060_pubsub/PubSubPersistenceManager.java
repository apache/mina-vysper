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

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public interface PubSubPersistenceManager extends StorageProvider {

	// CollectionNode methods
	public LeafNode findNode(Entity jid);

	public boolean containsNode(Entity jid);
	
	public void storeNode(Entity jid, LeafNode node);

	// LeafNode methods (subscriptions)
	public void addSubscriber(Entity nodeJID, String subscriptionID, Entity subscriber);

	public boolean containsSubscriber(Entity nodeJID, Entity subscriber);

	public boolean containsSubscriber(Entity nodeJID, String subscriptionId);

	public Entity getSubscriber(Entity nodeJID, String subscriptionId);

	public boolean removeSubscription(Entity nodeJID, String subscriptionId);

	public boolean removeSubscriber(Entity nodeJID, Entity subscriber);

	public int countSubscriptions(Entity nodeJID, Entity subscriber);

	public int countSubscriptions(Entity nodeJID);

	public void addMessage(Entity nodeJID, String messageID, XMLElement item);

	public void accept(Entity nodeJID, SubscriberVisitor subscriberVisitor);

}
