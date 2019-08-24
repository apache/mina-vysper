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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AffiliationItem;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeAffiliationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * This class handles the "affiliations" use cases for the "pubsub" namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "5.7", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PubSubRetrieveAffiliationsHandler extends AbstractPubSubGeneralHandler {

    /**
     * Creates a new handler for affiliations requests of a user.
     *
     * @param serviceConfiguration
     */
    public PubSubRetrieveAffiliationsHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "subscriptions" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "affiliations";
    }

    /**
     * This method takes care of handling the "affiliations" use-case
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

        List<AffiliationItem> subscriptions = collectAffiliations(root, sender);

        buildSuccessStanza(sb, subscriptions);

        sb.endInnerElement(); // pubsub
        return Collections.singletonList(new IQStanza(sb.build()));
    }

    /**
     * Traverses through all nodes to collect all affiliations of the user.
     * @return the list of subscriptions or an empty list.
     */
    private List<AffiliationItem> collectAffiliations(CollectionNode root, Entity sender) {
        List<AffiliationItem> affiliations;
        NodeAffiliationVisitor nodeAffiliationVisitor = new NodeAffiliationVisitor(sender);
        root.acceptNodes(nodeAffiliationVisitor);
        affiliations = nodeAffiliationVisitor.getAffiliations();
        return affiliations;
    }

    /**
     * This method adds the "affiliations" and eventual "affiliation" elements to the given StanzaBuilder.
     */
    private void buildSuccessStanza(StanzaBuilder sb, List<AffiliationItem> affiliations) {
        sb.startInnerElement("affiliations", NamespaceURIs.XEP0060_PUBSUB);

        for (AffiliationItem s : affiliations) {
            sb.startInnerElement("affiliation", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", s.getNodeName());
            sb.addAttribute("affiliation", s.getAffiliation().toString());
            sb.endInnerElement();
        }

        sb.endInnerElement();
    }
}
