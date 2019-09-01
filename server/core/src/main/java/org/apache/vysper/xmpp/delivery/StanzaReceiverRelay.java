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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.LocalRecipientOfflineException;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * relays stanzas to a StanzaReceiver identified by an Entity this relay is
 * mostly for testing purposes
 */
public class StanzaReceiverRelay implements StanzaRelay {

    private final Map<Entity, StanzaReceiverQueue> receiverMap = new HashMap<>();

    private boolean exploitFailureStrategy = true;

    private int countRelayed = 0;

    private int countFailed = 0;

    private int countDelivered = 0;

    private final AtomicBoolean acceptingMode = new AtomicBoolean(true);

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
    }

    /**
     * add new receiver
     */
    public void add(Entity receiverID, StanzaReceiverQueue receiver) {
        receiverMap.put(receiverID, receiver);
    }

    public void relay(InternalSessionContext sessionContext, Entity receiver, Stanza stanza,
					  DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException {
        if (!isRelaying()) {
            throw new ServiceNotAvailableException("relay is not relaying");
        }
        countRelayed++;
        if (receiver == null)
            throw new DeliveryException("receiver cannot be NULL");
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
    
    public Stanza nextStanza(){
        return receiverMap.values()
                .stream()
                .map(StanzaReceiverQueue::getNext)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public boolean isRelaying() {
        return acceptingMode.get();
    }

    public void stop() {
        acceptingMode.set(false);
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

    /**
     * all receiver queues are emptied
     */
    public void resetAll() {
        synchronized (receiverMap) {
            for (StanzaReceiverQueue receiver : receiverMap.values()) {
                // emptying by retrieving all stanzas from the queue
                while (receiver.getNext() != null) {
                    // continue
                }

            }
        }
        countRelayed = 0;
        countDelivered = 0;
    }
}
