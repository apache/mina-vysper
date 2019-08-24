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
package org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.handler;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

import java.util.Collections;
import java.util.List;

/**
 * handles jabber:iq:auth request - by returning "service unavailable"
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0078", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class AuthCompatibilityIQHandler extends IQHandler {

    @Override
    public boolean verify(Stanza stanza) {
        return super.verify(stanza) && verifyInnerNamespace(stanza, NamespaceURIs.JABBER_IQ_AUTH_COMPATIBILITY);
    }

    @Override
    protected List<Stanza> executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
										  SessionContext sessionContext, StanzaBroker stanzaBroker) {

        // from XEP 78 - http://www.xmpp.org/extensions/xep-0078.html:
        // If the server does not support non-SASL authentication (e.g., because it supports only SASL authentication
        // as defined in RFC 3920), it MUST return a <service-unavailable/> error. If the client previously attempted
        // SASL authentication but that attempt failed, the server MUST return a <policy-violation/> stream error
        // (see RFC 3920 regarding stream error syntax).

        switch (stanza.getIQType()) {

        case GET:
        case SET:
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE, stanza,
                    StanzaErrorType.CANCEL, "jabber:iq:auth not supported", "en", null));

        case RESULT:
        case ERROR:
            break; // ignore errors in compatibility only namespace
        }

        return null;
    }

}
