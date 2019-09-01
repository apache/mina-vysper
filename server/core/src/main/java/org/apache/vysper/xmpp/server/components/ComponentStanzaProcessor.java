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
package org.apache.vysper.xmpp.server.components;

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 */
public class ComponentStanzaProcessor implements StanzaProcessor {

    protected StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory;

    protected ComponentStanzaHandlerLookup componentStanzaHandlerLookup = new ComponentStanzaHandlerLookup();

    public ComponentStanzaProcessor(StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory) {
        this.stanzaHandlerExecutorFactory = requireNonNull(stanzaHandlerExecutorFactory);
    }

    public void addHandler(StanzaHandler stanzaHandler) {
        componentStanzaHandlerLookup.addDefaultHandler(stanzaHandler);
    }

    public void addDictionary(NamespaceHandlerDictionary namespaceHandlerDictionary) {
        componentStanzaHandlerLookup.addDictionary(namespaceHandlerDictionary);
    }

    public void processStanza(ServerRuntimeContext serverRuntimeContext, InternalSessionContext sessionContext, Stanza stanza,
							  SessionStateHolder sessionStateHolder) {
        if (stanza == null)
            throw new RuntimeException("cannot process NULL stanzas");

        XMPPCoreStanza xmppStanza = XMPPCoreStanza.getWrapper(stanza);
        if (xmppStanza == null)
            throw new RuntimeException("cannot process only: IQ, message or presence");

        StanzaHandler stanzaHandler = componentStanzaHandlerLookup.getHandler(xmppStanza);

        if (stanzaHandler == null) {
            throw new RuntimeException("no handler for stanza");
        }

        try {
            stanzaHandlerExecutorFactory.build(stanzaHandler).execute(stanza, serverRuntimeContext, false,
                    sessionContext, sessionStateHolder);
        } catch (ProtocolException e) {
            // TODO handle
            e.printStackTrace();
        }

    }

    public void processTLSEstablished(InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        throw new RuntimeException("should not be called for components, which only acts as an established session");
    }
}
