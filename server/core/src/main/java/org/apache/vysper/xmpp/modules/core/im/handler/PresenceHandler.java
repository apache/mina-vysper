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

import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.isSubscriptionType;

import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManagerUtils;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

import java.util.Collections;
import java.util.List;

/**
 * handling presence stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PresenceHandler extends XMPPCoreStanzaHandler {

    private final static PresenceAvailabilityHandler AVAILABILITY_HANDLER = new PresenceAvailabilityHandler();

    private final static PresenceSubscriptionHandler SUBSCRIPTION_HANDLER = new PresenceSubscriptionHandler();

    public String getName() {
        return "presence";
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return PresenceStanza.isOfType(stanza);
    }

    @Override
    public boolean isSessionRequired() {
        return false;
    }

    @Override
    protected List<Stanza> executeCore(XMPPCoreStanza stanza, ServerRuntimeContext serverRuntimeContext,
                                       boolean isOutboundStanza, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        PresenceStanza presenceStanza = (PresenceStanza) stanza;

        boolean subscriptionRelated = isSubscriptionType(presenceStanza.getPresenceType());

        RosterManager rosterManager = RosterManagerUtils.getRosterInstance(serverRuntimeContext, sessionContext);

        if (!subscriptionRelated)
            return Collections.singletonList(AVAILABILITY_HANDLER.executeCorePresence(serverRuntimeContext, isOutboundStanza, sessionContext,
                    presenceStanza, rosterManager, stanzaBroker));
        else
            return Collections.singletonList(SUBSCRIPTION_HANDLER.executeCorePresence(serverRuntimeContext, isOutboundStanza, sessionContext,
                    presenceStanza, rosterManager, stanzaBroker));
    }

}
