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

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.ReturnErrorToSenderFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
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
 * handles IQ info queries
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliance(compliant = {
        @SpecCompliant(spec = "xep-0030", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL, comment = "handles disco info queries"),
        @SpecCompliant(spec = "xep-0128", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE, comment = "allows InfoDataForm elements") })
public class DiscoInfoIQHandler extends DefaultIQHandler {

    final Logger logger = LoggerFactory.getLogger(DiscoInfoIQHandler.class);

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query")
                && verifyInnerNamespace(stanza, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {
        ServiceCollector serviceCollector = null;

        // TODO if the target entity does not exist, return error/cancel/item-not-found
        // TODO more strictly, server can also return error/cancel/service-unavailable

        // retrieve the service collector
        try {
            serviceCollector = (ServiceCollector) serverRuntimeContext.getServerRuntimeContextService(
                    ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
        } catch (Exception e) {
            logger.error("error retrieving ServiceCollector service {}", e);
            serviceCollector = null;
        }

        if (serviceCollector == null) {
            return Collections.singletonList(
                    ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR, stanza,
                            StanzaErrorType.CANCEL, "cannot retrieve IQ-get-info result from internal components",
                            getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        // if "vysper.org" is the server entity, 'to' can either be "vysper.org",
        // "node@vysper.org", "service.vysper.org".
        Entity to = stanza.getTo();
        boolean isServerInfoRequest = false;
        boolean isComponentInfoRequest = false;
        Entity serverEntity = serverRuntimeContext.getServerEntity();
        if (to == null || to.equals(serverEntity)) {
            isServerInfoRequest = true; // this can only be meant to query the server
        } else if (serverRuntimeContext.hasComponentStanzaProcessor(to)) {
            isComponentInfoRequest = true; // this is a query to a component
        } else if (!to.isNodeSet()) {
            isServerInfoRequest = serverEntity.equals(to);
            if (!isServerInfoRequest) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(
                        StanzaErrorCondition.ITEM_NOT_FOUND, stanza, StanzaErrorType.CANCEL,
                        "server does not handle info query requests for " + to.getFullQualifiedName(),
                        getErrorLanguage(serverRuntimeContext, sessionContext), null));
            }
        }

        XMLElement queryElement = stanza.getFirstInnerElement();
        String node = queryElement != null ? queryElement.getAttributeValue("node") : null;

        // collect all the info response elements
        List<InfoElement> elements = null;
        try {
            Entity from = stanza.getFrom();
            if (from == null)
                from = sessionContext.getInitiatingEntity();
            if (isServerInfoRequest) {
                elements = serviceCollector.processServerInfoRequest(new InfoRequest(from, to, node, stanza.getID()));
            } else if (isComponentInfoRequest) {
                elements = serviceCollector
                        .processComponentInfoRequest(new InfoRequest(from, to, node, stanza.getID()), stanzaBroker);
            } else {
                // "When an entity sends a disco#info request to a bare JID
                // (<account@domain.tld>) hosted by a server,
                // the server itself MUST reply on behalf of the hosted account, either with an
                // IQ-error or an IQ-result"
                if (to.isResourceSet()) {
                    relayOrWrite(stanza, sessionContext, stanzaBroker);
                    return Collections.emptyList();
                } else {
                    elements = serviceCollector.processInfoRequest(new InfoRequest(from, to, node, stanza.getID()));
                }
            }
        } catch (ServiceDiscoveryRequestException e) {
            // the request yields an error
            StanzaErrorCondition stanzaErrorCondition = e.getErrorCondition();
            if (stanzaErrorCondition == null)
                stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
            return Collections.singletonList(ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza,
                    StanzaErrorType.CANCEL, "disco info request failed.",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        // TODO check that elementSet contains at least one identity element and on
        // feature element!

        // render the stanza with information collected
        StanzaBuilder stanzaBuilder = StanzaBuilder
                .createIQStanza(to, stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .startInnerElement("query", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
        if (node != null) {
            stanzaBuilder.addAttribute("node", node);
        }
        for (InfoElement infoElement : elements) {
            infoElement.insertElement(stanzaBuilder);
        }

        stanzaBuilder.endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }

    @Override
    protected List<Stanza> handleResult(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {

        if (stanza.getTo().isNodeSet()) {
            relayOrWrite(stanza, sessionContext, stanzaBroker);
            return Collections.emptyList();
        } else {
            return super.handleResult(stanza, serverRuntimeContext, sessionContext, stanzaBroker);
        }
    }

    private void relayOrWrite(IQStanza stanza, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        boolean isOutbound = !sessionContext.getInitiatingEntity().equals(stanza.getTo().getBareJID());
        if (isOutbound) {
            try {
                Entity from = stanza.getFrom();
                if (from == null) {
                    from = sessionContext.getInitiatingEntity();
                }
                Stanza forward = StanzaBuilder.createForwardStanza(stanza, from, null);

                stanzaBroker.write(stanza.getTo(), forward, new ReturnErrorToSenderFailureStrategy(stanzaBroker));
            } catch (DeliveryException e) {
                logger.warn("relaying IQ failed", e);
            }
        } else {
            stanzaBroker.writeToSession(stanza);
        }
    }
}
