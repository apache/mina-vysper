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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * a relay which does not relay anything but simply records the sequence of entity/stanza pairs received
 * and makes it accessible for debugging or testing purposes.
 * for a little bit more advanced testing relay see StanzaReceiverRelay.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RecordingStanzaRelay implements StanzaRelay {

    private final ArrayList<Triple> entityStanzaPairs = new ArrayList<Triple>();

    protected final AtomicBoolean isRelaying = new AtomicBoolean(true);

    public void relay(InternalSessionContext sessionContext, Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        if (!isRelaying()) throw new ServiceNotAvailableException("recording stanza relay is not relaying");
        entityStanzaPairs.add(new Triple(receiver, stanza, deliveryFailureStrategy));
    }

    public boolean isRelaying() {
        return isRelaying.get();
    }

    public void stop() {
        this.isRelaying.set(false);
    }

    public Iterator<Triple> iterator() {
        return entityStanzaPairs.iterator();
    }

    public void reset() {
        entityStanzaPairs.clear();
    }

    /**
     * to easily set mode to accept all receivers (default) or to decline all
     * this is useful for setting the behavior when testing
     */
    public void setRelaying(boolean accepting) {
        this.isRelaying.set(accepting);
    }

    public static class Triple {
        private Entity entity;

        private Stanza stanza;

        private DeliveryFailureStrategy deliveryFailureStrategy;

        Triple(Entity entity, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) {
            this.entity = entity;
            this.stanza = stanza;
            this.deliveryFailureStrategy = deliveryFailureStrategy;
        }

        public Entity getEntity() {
            return entity;
        }

        public Stanza getStanza() {
            return stanza;
        }

        public DeliveryFailureStrategy getDeliveryFailureStrategy() {
            return deliveryFailureStrategy;
        }
    }
}
