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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * This visitor sends each visited entity the XMLElement specified via the constructor.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class SubscriberNotificationVisitor implements SubscriberVisitor {

    // Ignore all failures during the delivery (fire and forget)
    private DeliveryFailureStrategy dfs = new IgnoreFailureStrategy();
    // The StanzaRelay we use to send the messages
    private StanzaRelay stanzaRelay;
    // The payload.
    private XMLElement item;

    /**
     * Initialize the visitor with the StanzaRelay and Payload.
     * @param stanzaRelay relay for sending the messages.
     * @param item payload for the messages.
     */
    public SubscriberNotificationVisitor(StanzaRelay stanzaRelay, XMLElement item) {
        this.stanzaRelay = stanzaRelay;
        this.item = item;
    }

    /**
     * Send each visited subscriber a notification with the configured payload included.
     * 
     * @param nodeJID the node from which the message comes from
     * @param subscriber the receiver of the notification
     */
    public void visit(Entity nodeJID, Entity subscriber) {
        Stanza event = createMessageEventStanza(nodeJID, subscriber, "en", item); // TODO extract the hardcoded "en"

        try {
            stanzaRelay.relay(subscriber, event, dfs);
        } catch (DeliveryException e1) {
            // TODO we don't care - do we?
        }
    }

    /**
     * Creates the stanza for notifying the subscriber including payload.
     * 
     * @param from the node JID which sends the notification
     * @param to the receiver of the notification (subscriber)
     * @param lang the language of the stanza text-content.
     * @param item the payload as XMLElement
     * @return the prepared Stanza object.
     */
    private Stanza createMessageEventStanza(Entity from, Entity to, String lang, XMLElement item) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("message");
        stanzaBuilder.addAttribute("from", from.getBareJID().getFullQualifiedName());
        stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        stanzaBuilder.addAttribute("xml:lang", lang);
        stanzaBuilder.startInnerElement("event", NamespaceURIs.XEP0060_PUBSUB_EVENT);
        stanzaBuilder.addPreparedElement(item);
        return stanzaBuilder.getFinalStanza();
    }

}
