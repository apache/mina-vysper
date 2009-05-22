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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.compliance.SpecCompliant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * handles IQ info queries
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec="xep-0030", status= SpecCompliant.ComplianceStatus.IN_PROGRESS,
               coverage = SpecCompliant.ComplianceCoverage.PARTIAL, comment = "handles disco info queries")
public class DiscoInfoIQHandler extends DefaultIQHandler {

    final Logger logger = LoggerFactory.getLogger(DiscoInfoIQHandler.class);

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query");
    }

    @Override
    protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        ServiceCollector serviceCollector = null;

        // TODO if the target entity does not exist, return error/cancel/item-not-found
        // TODO more strictly, server can also return error/cancel/service-unaivable

        // retrieve the service collector
        try {
            serviceCollector = (ServiceCollector)serverRuntimeContext.getServerRuntimeContextService(ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
        } catch (Exception e) {
            logger.error("error retrieving ServiceCollector service {}", e);
            serviceCollector = null;
        }

        if (serviceCollector == null) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR, stanza,
                    StanzaErrorType.CANCEL,
                    "cannot retrieve IQ-get-info result from internal components",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null);
        }

        // if "vysper.org" is the server entity, 'to' can either be "vysper.org", "node@vysper.org", "service.vysper.org".
        Entity to = stanza.getTo();
        boolean isServerInfoRequest = false;
        Entity serviceEntity = serverRuntimeContext.getServerEnitity();
        if (to == null || to.equals(serviceEntity)) {
            isServerInfoRequest = true; // this can only be meant to query the server
        } else if (!to.isNodeSet() && !to.getDomain().endsWith(serviceEntity.getDomain())) {
            isServerInfoRequest = serviceEntity.equals(to);
            if (!isServerInfoRequest) {
                return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND, stanza,
                        StanzaErrorType.CANCEL,
                        "server does not handle info query requests for " + to.getFullQualifiedName(),
                        getErrorLanguage(serverRuntimeContext, sessionContext), null);
            }
        }

        XMLElement queryElement = stanza.getFirstInnerElement();
        String node = queryElement != null ? queryElement.getAttributeValue("node") : null;

        // collect all the info response elements
        List<InfoElement> elements = null;
        try {
            if (isServerInfoRequest) {
                elements = serviceCollector.processServerInfoRequest(new InfoRequest(stanza.getFrom(), to, node));
            } else {
                elements = serviceCollector.processInfoRequest(new InfoRequest(stanza.getFrom(), to, node));
            }
        } catch (ServiceDiscoveryRequestException e) {
            // the request yields an error
            StanzaErrorCondition stanzaErrorCondition = e.getErrorCondition();
            if (stanzaErrorCondition == null) stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
            return ServerErrorResponses.getInstance().getStanzaError(stanzaErrorCondition, stanza,
                    StanzaErrorType.CANCEL,
                    "disco info request failed.",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null);
        }

        //TODO check that elementSet contains at least one identity element and on feature element!

        // render the stanza with information collected
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(to, stanza.getFrom(), IQStanzaType.RESULT, stanza.getID()).
            startInnerElement("query").
            addNamespaceAttribute(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
            if (node != null) {
                stanzaBuilder.addAttribute("node", node);
            }
            for (InfoElement infoElement : elements) {
                infoElement.insertElement(stanzaBuilder);
            }

        stanzaBuilder.endInnerElement();

        return stanzaBuilder.getFinalStanza();
    }
}
