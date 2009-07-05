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

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.DuplicateNodeException;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;


/**
 * This handler is responsible for dealing with the "create" requests (in the pubsub namespace).
 * Even though this sounds like a "owner" action, it actually lies within the general "pubsub" namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", section="8.1", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class PubSubCreateNodeHandler extends AbstractPubSubGeneralHandler {

    /**
     * @param root
     */
    public PubSubCreateNodeHandler(CollectionNode root) {
        super(root);
    }

    /**
     * @return "create" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "create";
    }

    /**
     * This method takes care of handling the "create" use-case including all (relevant) error conditions.
     * 
     * @return the appropriate response stanza (either success or some error condition).
     */
    @Override
    @SpecCompliant(spec="xep-0060", section="8.1", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
    protected Stanza handleSet(IQStanza stanza,
            ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        Entity sender = getFromAddress(stanza, sessionContext);
        Entity receiver = stanza.getTo();

        String iqStanzaID = stanza.getAttributeValue("id");

        StanzaBuilder sb = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.RESULT, iqStanzaID);

        String nodeName = extractNodeName(stanza);

        try {
            root.createNode(receiver, nodeName);
        } catch (DuplicateNodeException e) {
            return errorStanzaGenerator.generateDuplicateNodeErrorStanza(sender, receiver, stanza);
        }

        return new IQStanza(sb.getFinalStanza());
    }
}
