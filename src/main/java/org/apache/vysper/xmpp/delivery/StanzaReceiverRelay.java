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
package org.apache.vysper.xmpp.delivery;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

import java.util.Map;
import java.util.HashMap;

/**
 * relays stanzas to a StanzaReceiver identified by an Entity
 * this relay is mostly for testing purposes
 */
public class StanzaReceiverRelay implements StanzaRelay {

    private final Map<Entity, StanzaReceiver> receiverMap = new HashMap<Entity, StanzaReceiver>();
    private boolean exploitFailureStrategy = true;
    private ServerRuntimeContext serverRuntimeContext = null;
    private int countRelayed = 0;
    private int countFailed = 0;
    private int countDelivered = 0;

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void add(Entity receiverID, StanzaReceiver receiver) {
        receiverMap.put(receiverID, receiver);
    }

    public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException {
        countRelayed++;
        if (receiver == null) throw new DeliveryException("receiver cannot be NULL");
        if (receiverMap.get(receiver) == null) {

            if (deliveryFailureStrategy != null && exploitFailureStrategy) {
                countFailed++;
                deliveryFailureStrategy.process(stanza, null);
                // TODO needs return here?
            }

            throw new LocalRecipientOfflineException("cannot find receiver " + receiver.getFullQualifiedName());
        }

        countDelivered++;
        receiverMap.get(receiver).deliver(stanza);
    }

    public int getCountRelayed() {
        return countRelayed;
    }

    public int getCountFailed() {
        return countFailed;
    }

    public int getCountDelivered() {
        return countDelivered;
    }

    public void resetAll() {
        synchronized (receiverMap) {
            for (StanzaReceiver receiver : receiverMap.values()) {
                if (receiver instanceof StanzaReceiverQueue) {
                    StanzaReceiverQueue stanzaReceiverQueue = (StanzaReceiverQueue) receiver;
                    // emptying by retrieving all stanzas from the queue
                    while (stanzaReceiverQueue.getNext() != null) {
                        // continue
                    }
                }

            }
        }
        countRelayed = 0;
        countDelivered = 0;
    }
}
