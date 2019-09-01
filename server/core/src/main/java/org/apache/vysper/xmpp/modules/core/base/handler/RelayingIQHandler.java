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
package org.apache.vysper.xmpp.modules.core.base.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.ReturnErrorToSenderFailureStrategy;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManagerUtils;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

import java.util.Collections;
import java.util.List;

/**
 * most IQ are targeted to the server. but sometimes, IQs are only routed through the server to another client.
 * this is the handler dealing with that. 
 */
public class RelayingIQHandler extends IQHandler {

    @Override
    protected List<Stanza> executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
                                          SessionContext sessionContext, StanzaBroker stanzaBroker) {
        // only handle IQs which are not directed to the server (vysper.org).
        // in the case where an IQ is send to the server, StanzaHandlerLookup.getIQHandler is responsible for
        // looking it up and we shouldn't have been come here in the first place.
        // but we might will relay to a component (chat.vysper.org)
        Entity to = stanza.getTo();
        if (to == null || to.equals(sessionContext.getServerJID())) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED,
                    stanza, StanzaErrorType.CANCEL, null, null, null));
        }

        RosterManager rosterManager = RosterManagerUtils.getRosterInstance(serverRuntimeContext, sessionContext);

        if (outboundStanza) {
            try {

                boolean toComponent = EntityUtils.isAddressingServerComponent(to, serverRuntimeContext.getServerEntity());

                Entity from = stanza.getFrom();
                if (from == null || !from.isResourceSet()) {
                    from = new EntityImpl(sessionContext.getInitiatingEntity(), serverRuntimeContext
                            .getResourceRegistry().getUniqueResourceForSession(sessionContext));
                }

                // determine if the is a matching subscription...
                boolean isFromContact = false;
                if (!toComponent) {
                    try {
                        isFromContact = rosterManager.retrieve(from.getBareJID()).getEntry(to.getBareJID()).hasFrom();
                    } catch (Exception e) {
                        isFromContact = false;
                    }
                }
                // deny relaying if neither isFromContact nor toComponent
                if (!isFromContact && !toComponent) {
                    return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE,
                            stanza, StanzaErrorType.CANCEL, null, null, null));
                }

                Stanza forwardedStanza = StanzaBuilder.createForward(stanza, from, null).build();
                stanzaBroker.write(to, forwardedStanza,
                        new ReturnErrorToSenderFailureStrategy(stanzaBroker));
            } catch (DeliveryException e) {
                // TODO how to handle this exception?
            }
        } else {
            // write inbound stanza to the user

            Entity from = stanza.getFrom();

            boolean fromComponent = (from != null) && EntityUtils.isAddressingServerComponent(from, serverRuntimeContext.getServerEntity());

            // determine if 'from' is a component or a matching subscription...
            boolean isToContact = false;
            if (!fromComponent) {
                try {
                    isToContact = rosterManager.retrieve(to.getBareJID()).getEntry(from.getBareJID()).hasTo();
                } catch (Exception e) {
                    isToContact = false;
                }
            }
            // ...otherwise relaying is denied
            if (!isToContact && !fromComponent) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE,
                        stanza, StanzaErrorType.CANCEL, null, null, null));
            }

            stanzaBroker.writeToSession(stanza);
        }

        return Collections.emptyList();
    }
}
