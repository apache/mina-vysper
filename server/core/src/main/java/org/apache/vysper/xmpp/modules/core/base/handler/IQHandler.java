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

package org.apache.vysper.xmpp.modules.core.base.handler;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.PARTIAL;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * handling IQ stanzas (request/response)
 * @see org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler for your convenient own IQ handler implementations
 * @see org.apache.vysper.xmpp.modules.core.base.handler.async.AbstractAsyncIQGetHandler for handling IQ gets asynchronously
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class IQHandler extends XMPPCoreStanzaHandler {

    public String getName() {
        return "iq";
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return IQStanza.isOfType(stanza);
    }

    protected boolean verifyInnerNamespace(Stanza stanza, String namespace) {
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        if (!xmlElementVerifier.subElementsPresentAtLeast(1))
            return false;

        List<XMLElement> innerElements = stanza.getInnerElements();
        XMLElement firstInnerElement = innerElements.get(0);

        return firstInnerElement.getNamespaceURI().equals(namespace);
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "rfc3920", section = "9.2.3", status = FINISHED, coverage = PARTIAL, comment = "covers points 1, 2, 5 and 6"),
            @SpecCompliant(spec = "rfc3920bis-09", section = "9.2.3", status = FINISHED, coverage = PARTIAL, comment = "covers points 1, 2, 5 and 6") })
    @Override
    protected List<Stanza> executeCore(XMPPCoreStanza coreStanza, ServerRuntimeContext serverRuntimeContext,
                                       boolean isOutboundStanza, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        IQStanza stanza = (IQStanza) coreStanza;

        // rfc3920/9.2.3/1.
        String id = stanza.getID();
        if (id == null) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY, "iq-stanza requires 'id' attribute to be present",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        // rfc3920/9.2.3/2.
        IQStanzaType iqType = stanza.getIQType();
        if (iqType == null) {
            // missing or unknown type
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY, "iq-stanza requires a valid 'type' attribute to be present",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null));
        }

        if (iqType == IQStanzaType.GET || iqType == IQStanzaType.SET) {
            // assure, set or get contain one and only one element
            // rfc3920/9.2.3/5.
            if (!coreStanza.getVerifier().subElementsPresentExact(1)) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                        StanzaErrorType.MODIFY, "iq stanza of type get or set require exactly one child",
                        getErrorLanguage(serverRuntimeContext, sessionContext), null));
            }
        } else if (iqType == IQStanzaType.RESULT) {
            // assure, result contains zero or one element
            // rfc3920/9.2.3/6.
            if (!coreStanza.getVerifier().subElementsPresentAtMost(1)) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                        StanzaErrorType.MODIFY, "iq stanza of type result may not have more than one child",
                        getErrorLanguage(serverRuntimeContext, sessionContext), null));
            }

        } else if (iqType == IQStanzaType.ERROR) {
            // this is handled for all types of stanzas down-stack
        }

        return executeIQLogic(stanza, serverRuntimeContext, isOutboundStanza, sessionContext, stanzaBroker);
    }

    protected String getErrorLanguage(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        if (sessionContext != null)
            return sessionContext.getXMLLang();
        return serverRuntimeContext.getDefaultXMLLang();
    }

    /**
     * must be overridden by specialized IQ handlers
     */
    protected List<Stanza> executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
                                          SessionContext sessionContext, StanzaBroker stanzaBroker) {
        // this is default behavior and must be replaced by overrider
        return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
                StanzaErrorType.CANCEL, null, null, null));
    }

}
