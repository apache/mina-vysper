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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NullPersistenceManager;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubPersistenceManager;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberNotificationVisitor;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class LeafNode {
	
	protected Entity jid;
	protected PubSubPersistenceManager storage = new NullPersistenceManager();
	
	/**
	 * Creates a new LeafNode with the specified JID.
	 * @param persistenceManager the persistence manager for loading additional information
	 * @param jid the JID of the node 
	 */
	public LeafNode(Entity jid) {
		this.jid = jid;
	}
	
	public void setPersistenceManager(PubSubPersistenceManager persistenceManager) {
		this.storage = persistenceManager;
	}
	
	public void subscribe(String id, Entity subscriber) {
		storage.addSubscriber(jid, id, subscriber);
	}
	
	public boolean isSubscribed(Entity subscriber) {
		return storage.containsSubscriber(jid, subscriber);
	}
	
	public boolean isSubscribed(String subscriptionID) {
		return storage.containsSubscriber(jid, subscriptionID);
	}
	
	public boolean unsubscribe(String subscriptionID, Entity subscriber) {
		Entity sub = storage.getSubscriber(jid, subscriptionID);

		if(sub != null && sub.equals(subscriber)) {
			return storage.removeSubscription(jid, subscriptionID);
		}
		return false;
	}
	
	public boolean unsubscribe(Entity subscriber) throws MultipleSubscriptionException {
		if(countSubscriptions(subscriber) > 1) {
			throw new MultipleSubscriptionException("Ambigous unsubscription request");
		}
		return storage.removeSubscriber(jid, subscriber);
	}
	
	public int countSubscriptions(Entity subscriber) {
		return storage.countSubscriptions(jid, subscriber);
	}

	public int countSubscriptions() {
		return storage.countSubscriptions(jid);
	}

	public void publish(Entity sender, StanzaRelay relay, String messageID, XMLElement item) {
		storage.addMessage(jid, messageID, item);
		sendMessageToSubscriber(relay, item);
	}
	
	protected void sendMessageToSubscriber(StanzaRelay stanzaRelay, XMLElement item) {
		storage.accept(jid, new SubscriberNotificationVisitor(stanzaRelay, item));
	}
}
