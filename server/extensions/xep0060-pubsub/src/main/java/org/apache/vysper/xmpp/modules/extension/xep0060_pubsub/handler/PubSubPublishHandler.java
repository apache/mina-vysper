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
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubPrivilege;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;


/**
 * This class handles the "publish" use cases for the "pubsub" namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", section="7.1", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PubSubPublishHandler extends AbstractPubSubGeneralHandler {

    /**
     * @param root
     */
    public PubSubPublishHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "publish" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "publish";
    }

    /**
     * This method takes care of handling the "publish" use-case including all (relevant) error conditions.
     * 
     * @return the appropriate response stanza (either success or some error condition).
     */
    @Override
    @SpecCompliance(compliant = {
            @SpecCompliant(spec="xep-0060", section="7.1.2", status= SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
            , @SpecCompliant(spec="xep-0060", section="7.1.2.1", status= SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
            , @SpecCompliant(spec="xep-0060", section="7.1.2.2", status= SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.1", status= SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.2", status= SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.3", status= SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.4", status= SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.5", status= SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
            , @SpecCompliant(spec="xep-0060", section="7.1.3.6", status= SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
        })
    protected Stanza handleSet(IQStanza stanza,
            ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        Entity serverJID = serviceConfiguration.getServerJID();
        CollectionNode root = serviceConfiguration.getRootNode();
        
        Entity sender = extractSenderJID(stanza, sessionContext);

        String iqStanzaID = stanza.getAttributeValue("id");

        StanzaBuilder sb = StanzaBuilder.createIQStanza(serverJID, sender, IQStanzaType.RESULT, iqStanzaID);
        sb.startInnerElement("pubsub");
        sb.addNamespaceAttribute(NamespaceURIs.XEP0060_PUBSUB);

        XMLElement publish = stanza.getFirstInnerElement().getFirstInnerElement(); // pubsub/publish
        String nodeName = publish.getAttributeValue("node"); // MUST

        XMLElement item = publish.getFirstInnerElement();
        String strID = item.getAttributeValue("id"); // MAY

        LeafNode node = root.find(nodeName);

        if(node == null) {
            // node does not exist - error condition 3 (7.1.3)
            return errorStanzaGenerator.generateNoNodeErrorStanza(sender, serverJID, stanza);
        }

        if(!node.isAuthorized(sender, PubSubPrivilege.PUBLISH)) {
            // not enough privileges to publish - error condition 1 (7.1.3)
            return errorStanzaGenerator.generateInsufficientPrivilegesErrorStanza(sender, serverJID, stanza);
        }

        if(strID == null) {
            strID = idGenerator.create();
            // wrap a new item element with the id attribute
            StanzaBuilder itemBuilder = new StanzaBuilder("item");
            itemBuilder.addAttribute("id", strID);
            itemBuilder.addPreparedElement(item.getFirstInnerElement());
            item = itemBuilder.getFinalStanza();
        }

        StanzaRelay relay = serverRuntimeContext.getStanzaRelay();
        node.publish(sender, relay, strID, item);

        buildSuccessStanza(sb, nodeName, strID);

        sb.endInnerElement(); // pubsub
        return new IQStanza(sb.getFinalStanza());
    }

    /**
     * This method adds the default "success" elements to the given StanzaBuilder.
     * 
     * @param sb the StanzaBuilder to add the success elements.
     * @param node the node to which the message was published.
     * @param id the id of the published message.
     */
    private void buildSuccessStanza(StanzaBuilder sb, String node, String id) {
        sb.startInnerElement("publish");
        sb.addAttribute("node", node);

        sb.startInnerElement("item");
        sb.addAttribute("id", id);
        sb.endInnerElement();

        sb.endInnerElement();
    }
}
