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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeSubscriberVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriptionItem;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * This class handles the "subscriptions" use cases for the "pubsub" namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "5.6", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PubSubRetrieveSubscriptionsHandler extends AbstractPubSubGeneralHandler {

    /**
     * Creates a new handler for subscriptions requests of a user.
     *
     * @param serviceConfiguration
     */
    public PubSubRetrieveSubscriptionsHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "subscriptions" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "subscriptions";
    }

    /**
     * This method takes care of handling the "subscriptions" use-case
     * 
     * @return the appropriate response stanza
     */
    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity serverJID = serviceConfiguration.getDomainJID();
        CollectionNode root = serviceConfiguration.getRootNode();

        Entity sender = extractSenderJID(stanza, sessionContext);
        String iqStanzaID = stanza.getAttributeValue("id");

        StanzaBuilder sb = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT);
        sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB);
        String nodeName = extractNodeName(stanza);

        List<SubscriptionItem> subscriptions = collectSubscriptions(root, sender, nodeName);

        buildSuccessStanza(sb, nodeName, subscriptions);

        sb.endInnerElement(); // pubsub
        return Collections.singletonList(new IQStanza(sb.build()));
    }

    /**
     * Traverses through all nodes or a single node to collect all subscriptions of the user.
     * @return the list of subscriptions or an empty list.
     */
    private List<SubscriptionItem> collectSubscriptions(CollectionNode root, Entity sender, String nodeName) {
        List<SubscriptionItem> subscriptions;
        if (nodeName == null) { // all subscriptions for all nodes
            NodeSubscriberVisitor nodeSubscriptionVisitor = new NodeSubscriberVisitor(sender);
            root.acceptNodes(nodeSubscriptionVisitor);
            subscriptions = nodeSubscriptionVisitor.getSubscriptions();
        } else { // only the subscriptions for the requested node
            LeafNode node = root.find(nodeName);

            if (node == null) {
                // done - this is only a filter - no error conditions are defined
                subscriptions = Collections.emptyList();
            } else {
                SubscriberSubscriptionVisitor subscriptionVisitor = new SubscriberSubscriptionVisitor(sender);
                node.acceptSubscribers(subscriptionVisitor);
                subscriptions = subscriptionVisitor.getSubscriptions();
            }
        }
        return subscriptions;
    }

    /**
     * This method adds the default "success" elements to the given StanzaBuilder.
     */
    private void buildSuccessStanza(StanzaBuilder sb, String nodeName, List<SubscriptionItem> subscriptions) {
        sb.startInnerElement("subscriptions", NamespaceURIs.XEP0060_PUBSUB);

        for (SubscriptionItem s : subscriptions) {
            sb.startInnerElement("subscription", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", s.getNodeName());
            sb.addAttribute("jid", s.getSubscriberJID().getFullQualifiedName());
            sb.addAttribute("subscription", s.getSubscriptionState());
            sb.addAttribute("subid", s.getSubscriptionID());
            sb.endInnerElement();
        }

        sb.endInnerElement();
    }
}
