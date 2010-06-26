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
package org.apache.vysper.xmpp.protocol.worker;

import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseWriter;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StateAwareProtocolWorker;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * high-level xmpp protocol logic, state-aware.
 * writes response stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractStateAwareProtocolWorker implements StateAwareProtocolWorker {

    abstract public SessionState getHandledState();

    public void processStanza(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza stanza,
            StanzaHandler stanzaHandler) {
        boolean proceed = checkState(sessionContext, sessionStateHolder, stanza, stanzaHandler);
        if (!proceed)
            return; // TODO close stream?

        ResponseStanzaContainer responseStanzaContainer = executeHandler(sessionContext, sessionStateHolder, stanza,
                stanzaHandler);

        writeResponse(sessionContext, responseStanzaContainer);
    }

    protected boolean checkState(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza stanza,
            StanzaHandler stanzaHandler) {
        return 0 == getHandledState().compareTo(sessionContext.getState());
    }

    protected void writeResponse(SessionContext sessionContext, ResponseStanzaContainer responseStanzaContainer) {
        if (responseStanzaContainer != null && responseStanzaContainer.getResponseStanza() != null) {
            if (sessionContext == null) {
                throw new IllegalStateException("no session context to write stanza to: "
                        + responseStanzaContainer.getResponseStanza());
            }
            ResponseWriter.writeResponse(sessionContext, responseStanzaContainer);
        }
    }

    protected ResponseStanzaContainer executeHandler(SessionContext sessionContext,
            SessionStateHolder sessionStateHolder, Stanza stanza, StanzaHandler stanzaHandler) {
        ResponseStanzaContainer responseStanzaContainer = null;
        try {
            responseStanzaContainer = stanzaHandler.execute(stanza, sessionContext.getServerRuntimeContext(),
                    isProcessingOutboundStanzas(), sessionContext, sessionStateHolder);
        } catch (ProtocolException e) {
            ResponseWriter.handleProtocolError(e, sessionContext, stanza);
            return null;
        }
        return responseStanzaContainer;
    }

    protected boolean isProcessingOutboundStanzas() {
        return true;
    }
}
