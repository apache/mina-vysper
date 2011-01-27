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

import java.util.Collection;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class ExternalComponentStanzaProcessor implements StanzaProcessor {

    private Entity component;

    public ExternalComponentStanzaProcessor(Entity component) {
System.out.println(11111);
        this.component  = component;
    }

    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza,
            SessionStateHolder sessionStateHolder) {
        if (stanza == null)
            throw new RuntimeException("cannot process NULL stanzas");

        // rewrite namespace
        stanza = StanzaBuilder.rewriteNamespace(stanza, NamespaceURIs.JABBER_CLIENT, NamespaceURIs.JABBER_COMPONENT_ACCEPT);

        Collection<SessionContext> componentSessions = ((DefaultServerRuntimeContext)serverRuntimeContext).getSessions(component);
        
        

        if(!componentSessions.isEmpty()) {
            SessionContext componentSession = componentSessions.iterator().next();
            componentSession.getResponseWriter().write(stanza);
        } else {
            // component not connected
            // TODO implement
        }
    }

    public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        // do nothing
    }
}
