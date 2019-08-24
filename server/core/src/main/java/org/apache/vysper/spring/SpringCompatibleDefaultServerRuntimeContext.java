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
package org.apache.vysper.spring;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.server.AlterableComponentRegistry;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 */
public class SpringCompatibleDefaultServerRuntimeContext extends DefaultServerRuntimeContext {

    private static class StanzaRelayHull implements StanzaRelay {

        protected StanzaRelay stanzaRelay;

        public void setStanzaRelay(StanzaRelay stanzaRelay) {
            this.stanzaRelay = stanzaRelay;
        }

        public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
                throws DeliveryException {
            if (!isRelaying()) {
                throw new ServiceNotAvailableException("relay is not relaying");
            }
            stanzaRelay.relay(receiver, stanza, deliveryFailureStrategy);
        }

        public boolean isRelaying() {
            return stanzaRelay.isRelaying();
        }

        public void stop() {
            stanzaRelay.stop();
        }
    }

    public SpringCompatibleDefaultServerRuntimeContext(Entity serverEntity, ServerFeatures serverFeatures,
            List<HandlerDictionary> dictionaries, ResourceRegistry resourceRegistry,
            AlterableComponentRegistry componentRegistry) {
        super(serverEntity, new StanzaRelayHull(), componentRegistry, resourceRegistry, serverFeatures, dictionaries);
    }

    public void setStanzaRelay(StanzaRelay stanzaRelay) {
        ((StanzaRelayHull) getStanzaRelay()).setStanzaRelay(stanzaRelay);
    }

    public void setModules(List<Module> modules) {
        super.addModules(modules);
    }
}
