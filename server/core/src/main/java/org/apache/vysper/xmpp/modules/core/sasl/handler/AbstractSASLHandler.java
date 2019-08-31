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
package org.apache.vysper.xmpp.modules.core.sasl.handler;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;
import org.apache.vysper.xmpp.modules.core.sasl.SASLFailureType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.protocol.exception.AuthenticationFailedException;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractSASLHandler implements StanzaHandler {
    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker)
            throws AuthenticationFailedException {
        if (!AuthorizationRetriesCounter.getFromSession(sessionContext).hasTriesLeft()) {
            AuthenticationFailedException failedException = new AuthenticationFailedException("too many retries");
            failedException.setErrorStanza(
                    ServerErrorResponses.getStreamError(StreamErrorCondition.POLICY_VIOLATION, null, null, null));
            throw failedException;
        }

        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        boolean saslNamespace = xmlElementVerifier.namespacePresent(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);

        if (!saslNamespace) {
            stanzaBroker.writeToSession(buildSASLFailure());
            return;
        }
        // the session must be in status ENCRYPTED. HOWEVER, only if encryption is not
        // required, SessionState.INITIATED
        // is fine, too.
        if (sessionStateHolder.getState() != SessionState.ENCRYPTED
                && !(sessionStateHolder.getState() != SessionState.INITIATED
                        && !serverRuntimeContext.getServerFeatures().isStartTLSRequired())) {
            stanzaBroker.writeToSession(buildSASLFailure());
            return;
        }

        stanzaBroker.writeToSession(executeWorker(stanza, sessionContext, sessionStateHolder));
    }

    protected Stanza buildSASLFailure() {
        return ServerErrorResponses.getSASLFailure(SASLFailureType.MALFORMED_REQUEST);
    }

    protected abstract Stanza executeWorker(Stanza stanza, SessionContext sessionContext,
            SessionStateHolder sessionStateHolder);
}
