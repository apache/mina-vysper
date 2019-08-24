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
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubPrivilege;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * This class is responsible for handling all "delete" stanzas within the
 * pubsub#owner namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "8.4", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PubSubOwnerDeleteNodeHandler extends AbstractPubSubOwnerHandler {

    /**
     * Create a new delete handler with the supplied configuration object.
     */
    public PubSubOwnerDeleteNodeHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "delete" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "delete";
    }

    /**
     * This method takes care of handling the "delete" use-case including all
     * (relevant) error conditions.
     * 
     * @return the appropriate response stanza (either success or some error
     *         condition).
     */
    @Override
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "xep-0060", section = "8.4.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.4.3.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE),
            @SpecCompliant(spec = "xep-0060", section = "8.4.3.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE) })
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity serverJID = serviceConfiguration.getDomainJID();
        CollectionNode root = serviceConfiguration.getRootNode();

        Entity sender = extractSenderJID(stanza, sessionContext);

        StanzaBuilder sb = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT);
        String nodeName = extractNodeName(stanza);
        LeafNode node = root.find(nodeName);

        if (node == null) {
            return Collections.singletonList(errorStanzaGenerator.generateNoNodeErrorStanza(sender, serverJID, stanza));
        }

        if (!node.isAuthorized(sender, PubSubPrivilege.DELETE)) {
            return Collections.singletonList(
                    errorStanzaGenerator.generateInsufficientPrivilegesErrorStanza(sender, serverJID, stanza));
        }

        sendDeleteNotifications(stanzaBroker, sender, nodeName, node);
        root.deleteNode(nodeName);

        return Collections.singletonList(new IQStanza(sb.build()));
    }

    /**
     * Creates and sends a notification for all subscribers that the node is going
     * to be deleted.
     * 
     * @param stanzaBroker
     * @param sender
     * @param nodeName
     * @param node
     */
    private void sendDeleteNotifications(StanzaBroker stanzaBroker, Entity sender, String nodeName, LeafNode node) {
        String strID = idGenerator.create();
        XMLElement delete = createDeleteElement(nodeName);
        node.publish(sender, stanzaBroker, strID, delete);
    }

    /**
     * Creates a XMLElement like this <delete node="nodeName"/>
     * 
     * @param nodeName
     *            the value for the node attribute
     * @return the XMLElement for inclusion in the delete notification.
     */
    private XMLElement createDeleteElement(String nodeName) {
        return new XMLElement(null, "delete", null, new Attribute[] { new Attribute("node", nodeName) },
                (XMLFragment[]) null);
    }

}
