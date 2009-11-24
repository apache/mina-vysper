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

import org.apache.vysper.xmpp.delivery.*;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.*;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.*;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.*;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.*;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ReturnErrorToSenderFailureStrategy implements DeliveryFailureStrategy {

    private StanzaRelay stanzaRelay;

    public ReturnErrorToSenderFailureStrategy(StanzaRelay stanzaRelay) {
        this.stanzaRelay = stanzaRelay;
    }

    @SpecCompliance(compliant = {
        @SpecCompliant(spec="rfc3921bis-08", section = "8.1", status = FINISHED, coverage = COMPLETE),
        @SpecCompliant(spec="rfc3921bis-08", section = "8.3.2", status = NOT_STARTED, coverage = UNKNOWN),
        @SpecCompliant(spec="rfc3921bis-08", section = "4.3", status = NOT_STARTED, coverage = UNKNOWN)
    })
    public void process(Stanza failedToDeliverStanza, DeliveryException deliveryException) throws DeliveryException {

        StanzaErrorCondition stanzaErrorCondition = StanzaErrorCondition.SERVICE_UNAVAILABLE;
        StanzaErrorType errorType = StanzaErrorType.CANCEL;

        if (!(failedToDeliverStanza instanceof XMPPCoreStanza)) {
            throw new DeliveryException("could not return to sender");
        }
        XMPPCoreStanza failedCoreStanza = (XMPPCoreStanza) failedToDeliverStanza;
        if (failedCoreStanza.getType() != null && failedCoreStanza.getType().equals("error")) {
            return; // do not answer these
        }

        if (deliveryException != null) {
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
                    if (presenceStanzaType == null ||
                        presenceStanzaType == SUBSCRIBED ||
                        presenceStanzaType == UNSUBSCRIBE ||
                        presenceStanzaType == UNSUBSCRIBED ||
                        presenceStanzaType == UNAVAILABLE ||
                        presenceStanzaType == ERROR) {
                        return; // silently ignore
                    }
                    // TODO what happens with PROBE? 8.1 is silent here, but see 4.3
                    if (presenceStanzaType == SUBSCRIBE) {
                        // return UNSUBSCRIBED
                        final Entity from = failedToDeliverStanza.getTo(); // reverse from/to
                        final Entity to = failedToDeliverStanza.getFrom(); // reverse from/to
                        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(from, to, null, UNSUBSCRIBED, null, null);
                        final Stanza finalStanza = builder.build();
                        stanzaRelay.relay(to, finalStanza, IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY);
                        return;
                    }
                }
            }
        }

        XMPPCoreStanza error = XMPPCoreStanza.getWrapper(ServerErrorResponses.getInstance().getStanzaError(stanzaErrorCondition, failedCoreStanza, errorType, "stanza could not be delivered", "en", null));
        stanzaRelay.relay(error.getTo(), error, IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY);
    }
}
