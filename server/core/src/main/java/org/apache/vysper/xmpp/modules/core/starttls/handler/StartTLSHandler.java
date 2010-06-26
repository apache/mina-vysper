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
package org.apache.vysper.xmpp.modules.core.starttls.handler;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StartTLSHandler implements StanzaHandler {
    public String getName() {
        return "starttls";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        return true;
    }

    public boolean isSessionRequired() {
        return true;
    }

    public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        boolean tlsNamespace = xmlElementVerifier.namespacePresent(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);

        if (!tlsNamespace) {
            return respondTLSFailure();
        }
        if (sessionStateHolder.getState() != SessionState.STARTED) {
            return respondTLSFailure();
        }

        Stanza responseStanza = new ServerResponses().getTLSProceed();

        // if all is correct, go to next phase
        sessionStateHolder.setState(SessionState.ENCRYPTION_STARTED);

        sessionContext.switchToTLS();

        return new ResponseStanzaContainerImpl(responseStanza);
    }

    private ResponseStanzaContainer respondTLSFailure() {
        return new ResponseStanzaContainerImpl(ServerErrorResponses.getInstance().getTLSFailure());
    }
}
