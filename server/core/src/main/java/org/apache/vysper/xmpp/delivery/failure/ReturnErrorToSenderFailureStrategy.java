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
package org.apache.vysper.xmpp.delivery.failure;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.UNKNOWN;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.NOT_STARTED;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.ERROR;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.SUBSCRIBE;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.SUBSCRIBED;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNAVAILABLE;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNSUBSCRIBE;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNSUBSCRIBED;

import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ReturnErrorToSenderFailureStrategy implements DeliveryFailureStrategy {

    private final StanzaBroker stanzaBroker;

    public ReturnErrorToSenderFailureStrategy(StanzaBroker stanzaBroker) {
        this.stanzaBroker = stanzaBroker;
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "rfc3921bis-08", section = "8.1", status = FINISHED, coverage = COMPLETE),
            @SpecCompliant(spec = "rfc3921bis-08", section = "8.3.2", status = NOT_STARTED, coverage = UNKNOWN),
            @SpecCompliant(spec = "rfc3921bis-08", section = "4.3", status = NOT_STARTED, coverage = UNKNOWN) })
    public void process(Stanza failedToDeliverStanza, List<DeliveryException> deliveryExceptions)
            throws DeliveryException {
        StanzaErrorCondition stanzaErrorCondition = StanzaErrorCondition.SERVICE_UNAVAILABLE;
        StanzaErrorType errorType = StanzaErrorType.CANCEL;

        XMPPCoreStanza failedCoreStanza = XMPPCoreStanza.getWrapper(failedToDeliverStanza);

        // Not a core stanza
        if (failedCoreStanza == null) {
            throw new DeliveryException("could not return to sender");
        }

        if ("error".equals(failedCoreStanza.getType())) {
            return; // do not answer these
        }

        if (deliveryExceptions == null) {
            XMPPCoreStanza error = XMPPCoreStanza.getWrapper(ServerErrorResponses.getStanzaError(stanzaErrorCondition,
                    failedCoreStanza, errorType, "stanza could not be delivered", "en", null));
            stanzaBroker.write(error.getTo(), error, IgnoreFailureStrategy.INSTANCE);
        } else if (deliveryExceptions.size() == 1) {
            DeliveryException deliveryException = deliveryExceptions.get(0);
            if (deliveryException instanceof LocalRecipientOfflineException) {
                // TODO implement 8.2.3 here
                stanzaErrorCondition = StanzaErrorCondition.RECIPIENT_UNAVAILABLE;
                if (failedCoreStanza instanceof MessageStanza || failedCoreStanza instanceof IQStanza) {
                    // RFC3921bis#8.1: message and IQ must return service unavailable
                    stanzaErrorCondition = StanzaErrorCondition.SERVICE_UNAVAILABLE;
                }
            } else if (deliveryException instanceof NoSuchLocalUserException) {
                // RFC3921bis#8.1: message and IQ must return service unavailable
                stanzaErrorCondition = StanzaErrorCondition.SERVICE_UNAVAILABLE;
                if (failedCoreStanza instanceof PresenceStanza) {
                    final PresenceStanzaType presenceStanzaType = ((PresenceStanza) failedCoreStanza).getPresenceType();
                    if (presenceStanzaType == null || presenceStanzaType == SUBSCRIBED
                            || presenceStanzaType == UNSUBSCRIBE || presenceStanzaType == UNSUBSCRIBED
                            || presenceStanzaType == UNAVAILABLE || presenceStanzaType == ERROR) {
                        return; // silently ignore
                    }
                    // TODO what happens with PROBE? 8.1 is silent here, but see 4.3
                    if (presenceStanzaType == SUBSCRIBE) {
                        // return UNSUBSCRIBED
                        final Entity from = failedToDeliverStanza.getTo(); // reverse from/to
                        final Entity to = failedToDeliverStanza.getFrom(); // reverse from/to
                        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(from, to, null, UNSUBSCRIBED, null,
                                null);
                        final Stanza finalStanza = builder.build();
                        stanzaBroker.write(to, finalStanza, IgnoreFailureStrategy.INSTANCE);
                        return;
                    }
                }
            } else if (deliveryException instanceof SmartDeliveryException) {
                // RFC3921bis#10.4.3: return remote server error to sender
                SmartDeliveryException smartDeliveryException = (SmartDeliveryException) deliveryException;
                XMPPCoreStanza error = XMPPCoreStanza.getWrapper(
                        ServerErrorResponses.getStanzaError(smartDeliveryException.getStanzaErrorCondition(),
                                failedCoreStanza, smartDeliveryException.getStanzaErrorType(),
                                smartDeliveryException.getErrorText(), "en", null));
                stanzaBroker.write(error.getTo(), error, IgnoreFailureStrategy.INSTANCE);
            }
        } else if (deliveryExceptions.size() > 1) {
            throw new RuntimeException("cannot return to sender for multiple failed deliveries");
        }
    }
}
