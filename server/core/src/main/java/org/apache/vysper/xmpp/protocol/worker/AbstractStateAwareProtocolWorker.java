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

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseWriter;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StateAwareProtocolWorker;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * high-level xmpp protocol logic, state-aware. writes response stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractStateAwareProtocolWorker implements StateAwareProtocolWorker {

    private final StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory;

    protected AbstractStateAwareProtocolWorker(StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory) {
        this.stanzaHandlerExecutorFactory = requireNonNull(stanzaHandlerExecutorFactory);
    }

    public abstract SessionState getHandledState();

    public void processStanza(ServerRuntimeContext serverRuntimeContext, InternalSessionContext sessionContext,
            SessionStateHolder sessionStateHolder, Stanza stanza, StanzaHandler stanzaHandler) {
        boolean proceed = checkState(sessionContext, sessionStateHolder, stanza, stanzaHandler);
        if (!proceed)
            return; // TODO close stream?

        executeHandler(serverRuntimeContext, sessionContext, sessionStateHolder, stanza, stanzaHandler);
    }

    protected boolean checkState(InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder,
            Stanza stanza, StanzaHandler stanzaHandler) {
        return getHandledState() == sessionStateHolder.getState();
    }

    private void executeHandler(ServerRuntimeContext serverRuntimeContext, InternalSessionContext sessionContext,
            SessionStateHolder sessionStateHolder, Stanza stanza, StanzaHandler stanzaHandler) {
        try {
            stanzaHandlerExecutorFactory.build(stanzaHandler).execute(stanza, serverRuntimeContext,
                    isProcessingOutboundStanzas(), sessionContext, sessionStateHolder);
        } catch (ProtocolException e) {
            ResponseWriter.handleProtocolError(e, sessionContext, stanza);
        }
    }

    protected boolean isProcessingOutboundStanzas() {
        return true;
    }
}
