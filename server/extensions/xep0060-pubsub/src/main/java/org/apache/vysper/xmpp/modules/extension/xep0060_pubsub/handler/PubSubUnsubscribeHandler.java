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

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.MultipleSubscriptionException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import java.util.Collections;
import java.util.List;

/**
 * This class handles the "unsubscribe" use cases for the "pubsub" namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "6.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class PubSubUnsubscribeHandler extends AbstractPubSubGeneralHandler {

    /**
     * Creates a new unsubscribe handler for users.
     *
     * @param serviceConfiguration
     */
    public PubSubUnsubscribeHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "unsubscribe" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "unsubscribe";
    }

    /**
     * This method takes care of handling the "unsubscribe" use-case including all (relevant) error conditions.
     * 
     * @return the appropriate response stanza (either success or some error condition).
     */
    @Override
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "xep-0060", section = "6.2.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "6.2.3.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "6.2.3.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "6.2.3.3", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "6.2.3.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "6.2.3.5", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE) })
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity serverJID = serviceConfiguration.getDomainJID();
        CollectionNode root = serviceConfiguration.getRootNode();

        Entity sender = extractSenderJID(stanza, sessionContext);
        Entity subJID = null;

        StanzaBuilder sb = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT);
        sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB);

        XMLElement unsub = stanza.getFirstInnerElement().getFirstInnerElement(); // pubsub/unsubscribe
        String strSubJID = unsub.getAttributeValue("jid"); // MUST
        String strSubID = unsub.getAttributeValue("subid"); // SHOULD (req. for more than one subscription)

        try {
            subJID = EntityImpl.parse(strSubJID);
        } catch (EntityFormatException e) {
            // return error stanza... (general error)
            return Collections.singletonList(errorStanzaGenerator.generateJIDMalformedErrorStanza(sender, serverJID, stanza));
        }

        if (!sender.getBareJID().equals(subJID.getBareJID())) {
            // insufficient privileges (error condition 3 (6.2.3))
            return Collections.singletonList(errorStanzaGenerator.generateInsufficientPrivilegesErrorStanza(sender, serverJID, stanza));
        }

        String nodeName = extractNodeName(stanza);
        LeafNode node = root.find(nodeName);

        if (node == null) {
            // no such node (error condition 4 (6.2.3))
            return Collections.singletonList(errorStanzaGenerator.generateNoNodeErrorStanza(sender, serverJID, stanza));
        }

        if (strSubID == null) {
            try {
                if (!node.unsubscribe(subJID)) {
                    // has no subscription (6.2.3.2)
                    return Collections.singletonList(errorStanzaGenerator.generateNoSuchSubscriberErrorStanza(sender, serverJID, stanza));
                }
            } catch (MultipleSubscriptionException e) {
                // error case 6.2.3.1
                return Collections.singletonList(errorStanzaGenerator.generateSubIDRequiredErrorStanza(sender, serverJID, stanza));
            }
        } else {
            if (!node.unsubscribe(strSubID, subJID)) {
                // subID not valid (6.2.3.5)
                return Collections.singletonList(errorStanzaGenerator.generateSubIDNotValidErrorStanza(sender, serverJID, stanza));
            }
        }

        sb.endInnerElement(); // pubsub
        return Collections.singletonList(new IQStanza(sb.build()));
    }

}
