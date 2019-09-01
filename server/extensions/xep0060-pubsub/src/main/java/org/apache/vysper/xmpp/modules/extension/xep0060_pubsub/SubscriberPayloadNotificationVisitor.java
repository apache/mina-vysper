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
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This visitor sends each visited entity the XMLElement specified via the
 * constructor.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "7.1.2.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class SubscriberPayloadNotificationVisitor implements SubscriberVisitor {
    final Logger logger = LoggerFactory.getLogger(SubscriberPayloadNotificationVisitor.class);

    // Ignore all failures during the delivery (fire and forget)
    private DeliveryFailureStrategy dfs = IgnoreFailureStrategy.INSTANCE;

    // The StanzaBroker we use to send the messages
    private final StanzaBroker stanzaBroker;

    // The payload.
    private XMLElement item;

    // The server JID
    private Entity serverJID;

    /**
     * Initialize the visitor with the StanzaRelay and payload.
     * 
     * @param stanzaBroker
     *            relay for sending the messages.
     * @param item
     *            payload for the messages.
     */
    public SubscriberPayloadNotificationVisitor(Entity serverJID, StanzaBroker stanzaBroker, XMLElement item) {
        this.serverJID = serverJID;
        this.stanzaBroker = stanzaBroker;
        this.item = item;
    }

    /**
     * Send each visited subscriber a notification with the configured payload
     * included.
     * 
     * @param nodeName
     *            the node from which the message comes from
     * @param subscriptionID
     *            the subscription ID
     * @param subscriber
     *            the receiver of the notification
     */
    public void visit(String nodeName, String subscriptionID, Entity subscriber) {
        Stanza event = createMessageEventStanza(nodeName, subscriber, "en", item); // TODO extract the hardcoded "en"

        try {
            stanzaBroker.write(subscriber, event, dfs);
        } catch (DeliveryException e1) {
            if (logger.isTraceEnabled())
                logger.trace("Couldn't deliver message to " + subscriber.getFullQualifiedName(), e1);
            // TODO we don't care - do we?
        }
    }

    /**
     * Creates the stanza for notifying the subscriber including payload.
     * 
     * @param to
     *            the receiver of the notification (subscriber)
     * @param lang
     *            the language of the stanza text-content.
     * @param item
     *            the payload as XMLElement
     * @return the prepared Stanza object.
     */
    private Stanza createMessageEventStanza(String nodeName, Entity to, String lang, XMLElement item) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("message", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("from", serverJID.getFullQualifiedName());
        stanzaBuilder.addAttribute("to", to.getFullQualifiedName());
        stanzaBuilder.addAttribute(NamespaceURIs.XML, "lang", lang);
        stanzaBuilder.startInnerElement("event", NamespaceURIs.XEP0060_PUBSUB_EVENT);
        stanzaBuilder.startInnerElement("items", NamespaceURIs.XEP0060_PUBSUB_EVENT);
        stanzaBuilder.addAttribute("node", nodeName);
        stanzaBuilder.addPreparedElement(item);
        stanzaBuilder.endInnerElement(); // items
        stanzaBuilder.endInnerElement(); // event
        return stanzaBuilder.build();
    }

}
