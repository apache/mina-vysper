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
package org.apache.vysper.xmpp.modules.servicediscovery.handler;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handles IQ items queries
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0030", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL, comment = "handles disco item queries")
public class DiscoItemIQHandler extends DefaultIQHandler {

    final Logger logger = LoggerFactory.getLogger(DiscoItemIQHandler.class);

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query");
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        ServiceCollector serviceCollector = null;

        // retrieve the service collector
        try {
            serviceCollector = (ServiceCollector) serverRuntimeContext
                    .getServerRuntimeContextService(ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
        } catch (Exception e) {
            logger.error("error retrieving ServiceCollector service {}", e);
            serviceCollector = null;
        }

        if (serviceCollector == null) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR,
                    stanza, StanzaErrorType.CANCEL, "cannot retrieve IQ-get-items result from internal components",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        Entity to = stanza.getTo();
        boolean isServerInfoRequest = false;
        boolean isComponentInfoRequest = false;
        if (to == null) {
            isServerInfoRequest = true; // this can only be meant to query the server
        } else if (!to.isNodeSet()) {
            isServerInfoRequest = serverRuntimeContext.getServerEntity().equals(to);
            isComponentInfoRequest = serverRuntimeContext.hasComponentStanzaProcessor(to);
            if (!isServerInfoRequest && !isComponentInfoRequest) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND, stanza,
                        StanzaErrorType.CANCEL,
                        "server does not handle items query requests for " + to.getFullQualifiedName(),
                        getErrorLanguage(serverRuntimeContext, sessionContext), null));
            }
        }

        XMLElement queryElement = stanza.getFirstInnerElement();
        String node = queryElement != null ? queryElement.getAttributeValue("node") : null;

        // collect all the item response elements
        List<Item> items;
        try {
            Entity from = stanza.getFrom();
            if (from == null) from = sessionContext.getInitiatingEntity(); 
            items = serviceCollector.processItemRequest(new InfoRequest(from, stanza.getTo(), node, stanza
                    .getID()), stanzaBroker);
        } catch (ServiceDiscoveryRequestException e) {
            // the request yields an error
            StanzaErrorCondition stanzaErrorCondition = e.getErrorCondition();
            if (stanzaErrorCondition == null)
                stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
            return Collections.singletonList(ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza,
                    StanzaErrorType.CANCEL, "disco info request failed.",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        // render the stanza with information collected
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(to, stanza.getFrom(), IQStanzaType.RESULT,
                stanza.getID()).startInnerElement("query", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
        if (node != null) {
            stanzaBuilder.addAttribute("node", node);
        }
        for (Item item : items) {
            item.insertElement(stanzaBuilder);
        }
        stanzaBuilder.endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }
}
