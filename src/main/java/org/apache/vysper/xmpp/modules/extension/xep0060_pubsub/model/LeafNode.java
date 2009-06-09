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

import java.util.Map;
import java.util.TreeMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class LeafNode {
	
	protected Entity jid;
	protected Map<String,Entity> subscribers;
	protected Map<String,XMLElement> messageStorage;
	
	/**
	 * Creates a new LeafNode with the specified name.
	 * @param name the name of the node 
	 */
	public LeafNode(Entity jid) {
		this.jid = jid;
		this.subscribers = new TreeMap<String, Entity>();
		this.messageStorage = new TreeMap<String, XMLElement>();
	}
	
	public void subscribe(String id, Entity subscriber) {
		subscribers.put(id,subscriber);
	}
	
	public boolean isSubscribed(Entity subscriber) {
		return subscribers.containsValue(subscriber);
	}
	
	public boolean isSubscribed(String subscriptionId) {
		return subscribers.containsKey(subscriptionId);
	}
	
	public boolean unsubscribe(String subscriptionId, Entity subscriber) {
		Entity sub = subscribers.get(subscriptionId);
		if(sub != null && sub.equals(subscriber)) {
			return subscribers.remove(subscriptionId) != null;
		}
		return false;
	}
	
	public boolean unsubscribe(Entity subscriber) throws MultipleSubscriptionException {
		if(countSubscriptions(subscriber) > 1) {
			throw new MultipleSubscriptionException("Ambigous unsubscription request");
		}
		return subscribers.values().remove(subscriber);
	}
	
	public int countSubscriptions(Entity subscriber) {
		int count = 0;
		for(Entity sub : subscribers.values()) {
			if(subscriber.equals(sub)) {
				++count;
			}
		}
		return count;
	}

	public int countSubscriptions() {
		return subscribers.size();
	}

	public void publish(Entity sender, StanzaRelay relay, String strID, XMLElement item) {
		messageStorage.put(strID, item);
		sendMessageToSubscriber(relay, item);
	}
	
	protected void sendMessageToSubscriber(StanzaRelay stanzaRelay, XMLElement item) {
		DeliveryFailureStrategy dfs = new IgnoreFailureStrategy();
		for(Entity sub : subscribers.values()) {
			Stanza event = createMessageEventStanza(jid, sub, "en", item); // TODO extract the hardcoded "en"
			// TODO filter sender of the item?
			
			try {
				stanzaRelay.relay(sub, event, dfs);
			} catch (DeliveryException e1) {
				// TODO we don't care - do we?
			}
		}
	}

	private Stanza createMessageEventStanza(Entity from, Entity to, String lang, XMLElement item) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("message");
        stanzaBuilder.addAttribute("from", from.getBareJID().getFullQualifiedName());
        stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        stanzaBuilder.addAttribute("xml:lang", lang);
        stanzaBuilder.startInnerElement("event", NamespaceURIs.XEP0060_PUBSUB_EVENT);
        stanzaBuilder.addPreparedElement(item);
        return stanzaBuilder.getFinalStanza();
	}
}
