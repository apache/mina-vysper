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
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * central component for delivering stanzas.
 * handles message by routing to another server/domain or delivers locally.
 * messages are processed immediately, so this is not a 'staging' StanzaRelay like the downstream internal and external relay.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaRelayBroker implements StanzaRelay {

    protected StanzaRelay internalRelay;

    protected StanzaRelay externalRelay;

    protected ServerRuntimeContext serverRuntimeContext;

    /**
     * a StanzaRelay receiving stanzas which are targeted to server-local JIDs
     */
    public void setInternalRelay(StanzaRelay internalRelay) {
        this.internalRelay = internalRelay;
    }

    /**
     * a StanzaRelay receiving stanzas which are targeted to JIDs which are external to this server
     */
    public void setExternalRelay(StanzaRelay externalRelay) {
        this.externalRelay = externalRelay;
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {

        boolean toServerTLD = receiver == null
                || (!receiver.isNodeSet() && EntityUtils.isAddressingServer(receiver, serverRuntimeContext.getServerEnitity()));
        
        boolean toComponent = receiver != null && EntityUtils.isAddressingServerComponent(receiver, serverRuntimeContext.getServerEnitity());
        if (toServerTLD) {
            // TODO handle by server

            // TODO if received <message/> from another server 'to' MUST be set
            // TODO if received <presence/> from another server with no 'to', broadcast to subscribed entities
            // TODO if received <iq/>/get/set with no 'to', see 3920bis#11.1.4

            throw new RuntimeException("server as the direct receiver of stanza not yet implemented");
            //return;
        }

        boolean relayToExternal = serverRuntimeContext.getServerFeatures().isRelayingToFederationServers();

        if (EntityUtils.isAddressingServer(receiver, serverRuntimeContext.getServerEnitity()) || toComponent) {
            internalRelay.relay(receiver, stanza, deliveryFailureStrategy);
        } else {
            if (!relayToExternal)
                throw new IllegalStateException("this server is not relaying to external currently");
            externalRelay.relay(receiver, stanza, deliveryFailureStrategy);
        }
    }

}
