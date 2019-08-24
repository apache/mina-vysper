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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AffiliationItem;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.CollectingMemberAffiliationVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubAffiliation;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubPrivilege;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LastOwnerResignedException;
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
 * This class is responsible for handling all "affiliations" stanzas within the pubsub#owner namespace.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "8.9", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PubSubOwnerManageAffiliationsHandler extends AbstractPubSubOwnerHandler {

    /**
     * Creates a new handler with the supplied configuration object.
     *
     * @param serviceConfiguration configuration object to use.
     */
    public PubSubOwnerManageAffiliationsHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "affiliations" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "affiliations";
    }

    /**
     * This method takes care of handling the "affiliations" use-case including all (relevant) error conditions.
     *
     * @return the appropriate response stanza (either success or some error condition).
     */
    @Override
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "xep-0060", section = "8.9.1.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.9.1.3.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.9.1.3.3", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.2", status = SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED),
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.3.3", status = SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED),
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.3.4", status = SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED) })
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity serverJID = serviceConfiguration.getDomainJID();
        CollectionNode root = serviceConfiguration.getRootNode();

        Entity sender = extractSenderJID(stanza, sessionContext);

        StanzaBuilder sb = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT);
        sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB_OWNER);

        String nodeName = extractNodeName(stanza);
        LeafNode node = root.find(nodeName);

        if (node == null) {
            return Collections.singletonList(errorStanzaGenerator.generateNoNodeErrorStanza(sender, serverJID, stanza));
        }

        if (!node.isAuthorized(sender, PubSubPrivilege.MANAGE_AFFILIATIONS)) {
            return Collections.singletonList(errorStanzaGenerator.generateInsufficientPrivilegesErrorStanza(sender, serverJID, stanza));
        }

        List<AffiliationItem> affiliations = collectAllAffiliations(node);
        buildSuccessStanza(sb, node, affiliations);

        sb.endInnerElement();
        return Collections.singletonList(new IQStanza(sb.build()));
    }

    /**
     * This method takes care of handling the "affiliations" use-case including all (relevant) error conditions.
     *
     * @return the appropriate response stanza (either success or some error condition).
     */
    @Override
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.PARTIAL),
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.3.3", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.9.2.3.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE) })
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity serverJID = serviceConfiguration.getDomainJID();
        CollectionNode root = serviceConfiguration.getRootNode();
        Entity sender = extractSenderJID(stanza, sessionContext);

        StanzaBuilder sb = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT);
        sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB_OWNER);

        String nodeName = extractNodeName(stanza);
        LeafNode node = root.find(nodeName);

        if (node == null) {
            return Collections.singletonList(errorStanzaGenerator.generateNoNodeErrorStanza(sender, serverJID, stanza));
        }

        if (!node.isAuthorized(sender, PubSubPrivilege.MANAGE_AFFILIATIONS)) {
            return Collections.singletonList(errorStanzaGenerator.generateInsufficientPrivilegesErrorStanza(sender, serverJID, stanza));
        }

        XMLElement affiliationElement = null;
        try {
            if (stanza.getFirstInnerElement().getFirstInnerElement().getInnerElements().size() != 1) {
                return Collections.singletonList(errorStanzaGenerator.generateNotAcceptableErrorStanza(serverJID, sender, stanza));
            }

            affiliationElement = stanza.getFirstInnerElement().getFirstInnerElement().getFirstInnerElement();

            Entity userJID = null;
            try {
                userJID = EntityImpl.parse(affiliationElement.getAttributeValue("jid"));
            } catch (EntityFormatException e) {
                return Collections.singletonList(errorStanzaGenerator.generateJIDMalformedErrorStanza(serverJID, sender, stanza)); // TODO not defined in the standard(?)
            }

            PubSubAffiliation newAffiliation = PubSubAffiliation.get(affiliationElement
                    .getAttributeValue("affiliation"));
            node.setAffiliation(userJID, newAffiliation);
        } catch (LastOwnerResignedException e) {
            // if the last owner tries to resign.
            return Collections.singletonList(errorStanzaGenerator.generateNotAcceptableErrorStanza(serverJID, sender, stanza));
        } catch (Throwable t) { // possible null-pointer
            return Collections.singletonList(errorStanzaGenerator.generateBadRequestErrorStanza(serverJID, sender, stanza)); // TODO not defined in the standard(?)
        }

        sb.endInnerElement();
        return Collections.singletonList(new IQStanza(sb.build()));
    }

    /**
     * Creates the stanza to be sent for successful requests.
     */
    private void buildSuccessStanza(StanzaBuilder sb, LeafNode node, List<AffiliationItem> affiliations) {
        sb.startInnerElement("affiliations", NamespaceURIs.XEP0060_PUBSUB);
        sb.addAttribute("node", node.getName());

        for (AffiliationItem i : affiliations) {
            sb.startInnerElement("affiliation", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("jid", i.getJID().getFullQualifiedName());
            sb.addAttribute("affiliation", i.getAffiliation().toString());
            sb.endInnerElement();
        }

        sb.endInnerElement();
    }

    /**
     * Creates a list of all affiliated users to the given node.
     */
    private List<AffiliationItem> collectAllAffiliations(LeafNode node) {
        List<AffiliationItem> affiliations;
        CollectingMemberAffiliationVisitor memberAffiliationVisitor = new CollectingMemberAffiliationVisitor(node
                .getName());
        node.acceptMemberAffiliations(memberAffiliationVisitor);
        affiliations = memberAffiliationVisitor.getAffiliations();
        return affiliations;
    }
}
