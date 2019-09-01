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
package org.apache.vysper.xmpp.modules.core.im.handler;

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractPresenceSpecializedHandler {

    final Logger logger = LoggerFactory.getLogger(AbstractPresenceSpecializedHandler.class);

    protected PresenceStanza buildPresenceStanza(Entity from, Entity to, PresenceStanzaType type,
            List<XMLElement> innerElements) {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(from, to, null, type, null, null);
        if (innerElements != null) {
            for (XMLElement innerElement : innerElements) {
                builder.addPreparedElement(innerElement);
            }
        }
        return (PresenceStanza) XMPPCoreStanza.getWrapper(builder.build());
    }

    protected void relayStanza(Entity receiver, Stanza stanza, StanzaBroker stanzaBroker) {
        try {
            stanzaBroker.write(receiver, stanza, IgnoreFailureStrategy.INSTANCE);
        } catch (DeliveryException e) {
            logger.warn("presence relaying failed ", e);
        }
    }

    abstract/* package */Stanza executeCorePresence(ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, PresenceStanza presenceStanza, RosterManager rosterManager,
            StanzaBroker stanzaBroker);

}
