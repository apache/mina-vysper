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
package org.apache.vysper.xmpp.protocol;

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
public class DefaultStanzaBroker implements StanzaBroker {

    private final StanzaRelay stanzaRelay;

    private final InternalSessionContext sessionContext;

    private final OfflineStanzaReceiver offlineStanzaReceiver;

    public DefaultStanzaBroker(StanzaRelay stanzaRelay, InternalSessionContext sessionContext,
            OfflineStanzaReceiver offlineStanzaReceiver) {
        this.stanzaRelay = requireNonNull(stanzaRelay);
        this.sessionContext = sessionContext;
        this.offlineStanzaReceiver = offlineStanzaReceiver;
    }
    
    public DefaultStanzaBroker(StanzaRelay stanzaRelay, InternalSessionContext sessionContext){
        this(stanzaRelay, sessionContext, null);
    }

    @Override
    public void write(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        if (stanza == null) {
            return;
        }
        stanzaRelay.relay(sessionContext, receiver, stanza, deliveryFailureStrategy);
    }

    @Override
    public void writeToSession(Stanza stanza) {
        if (stanza == null) {
            return;
        }
        if (sessionContext != null) {
            sessionContext.getResponseWriter().write(stanza);
            return;
        }
        if (offlineStanzaReceiver == null) {
            return;
        }
        offlineStanzaReceiver.receive(stanza);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultStanzaBroker that = (DefaultStanzaBroker) o;

        if (!stanzaRelay.equals(that.stanzaRelay)) {
            return false;
        }
        if (sessionContext != null ? !sessionContext.equals(that.sessionContext) : that.sessionContext != null) {
            return false;
        }
        return offlineStanzaReceiver != null ? offlineStanzaReceiver.equals(that.offlineStanzaReceiver)
                : that.offlineStanzaReceiver == null;
    }

    @Override
    public int hashCode() {
        int result = stanzaRelay.hashCode();
        result = 31 * result + (sessionContext != null ? sessionContext.hashCode() : 0);
        result = 31 * result + (offlineStanzaReceiver != null ? offlineStanzaReceiver.hashCode() : 0);
        return result;
    }
}
