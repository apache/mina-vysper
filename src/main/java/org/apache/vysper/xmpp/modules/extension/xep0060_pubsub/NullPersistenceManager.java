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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class NullPersistenceManager implements LeafNodeStorageProvider, CollectionNodeStorageProvider {

	protected Map<Entity, LeafNode> nodes;

	protected Map<String, Entity> subscribers;
	protected Map<String, XMLElement> messages;
	
	public NullPersistenceManager() {
		nodes = new HashMap<Entity, LeafNode>();
		
		this.subscribers = new TreeMap<String, Entity>();
		this.messages = new TreeMap<String, XMLElement>();
	}
	
	public LeafNode findNode(Entity jid) {
		return nodes.get(jid);
	}
	
	public boolean containsNode(Entity jid) {
		return nodes.containsKey(jid);
	}
	
	public void storeNode(Entity jid, LeafNode node) {
		nodes.put(jid, node);
	}
	/////////////////////////////////////////////////////////////////
	
	public void addSubscriber(Entity nodeJID, String subscriptionID, Entity subscriber) {
		subscribers.put(subscriptionID, subscriber);
	}

	public boolean containsSubscriber(Entity nodeJID, Entity subscriber) {
		return subscribers.containsValue(subscriber);
	}

	public boolean containsSubscriber(Entity nodeJID, String subscriptionId) {
		return subscribers.containsKey(subscriptionId);
	}

	public Entity getSubscriber(Entity nodeJID, String subscriptionId) {
		return subscribers.get(subscriptionId);
	}

	public boolean removeSubscription(Entity nodeJID, String subscriptionId) {
		return subscribers.remove(subscriptionId) != null;
	}

	public boolean removeSubscriber(Entity nodeJID, Entity subscriber) {
		return subscribers.values().remove(subscriber);
	}

	public int countSubscriptions(Entity nodeJID, Entity subscriber) {
		int count = 0;
		for(Entity sub : subscribers.values()) {
			if(subscriber.equals(sub)) {
				++count;
			}
		}
		return count;
	}

	public int countSubscriptions(Entity nodeJID) {
		return subscribers.size();
	}

	public void addMessage(Entity nodeJID, String messageID, XMLElement item) {
		messages.put(messageID, item);
	}

	public void acceptForEachSubscriber(Entity nodeJID, SubscriberVisitor subscriberVisitor) {
		for(Entity sub : subscribers.values()) {
			subscriberVisitor.visit(nodeJID, sub);
		}
	}
}
