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

package org.apache.vysper.xmpp.modules.extension.xep0114_component;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionContext.SessionTerminationCause;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HandshakeHandler implements StanzaHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(HandshakeHandler.class);

    
    public String getName() {
        return "handshake";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        String namespaceURI = stanza.getNamespaceURI();
        if (namespaceURI == null)
            return false;
        return namespaceURI.equals(NamespaceURIs.JABBER_COMPONENT_ACCEPT);
    }

    public boolean isSessionRequired() {
        return false;
    }

    public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {

        if(stanza.getInnerText() != null) {
            String handshake = stanza.getInnerText().getText();
            ComponentAuthentication componentAuthentication = (ComponentAuthentication) serverRuntimeContext.getStorageProvider(ComponentAuthentication.class);
            if(componentAuthentication != null) {
                if(componentAuthentication.verifyCredentials(sessionContext.getInitiatingEntity(), handshake, sessionContext.getSessionId())) {
                    sessionStateHolder.setState(SessionState.AUTHENTICATED);
                    
                    Stanza response = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).build();
                    return new ResponseStanzaContainerImpl(response);
                } else {
                    sessionContext.endSession(SessionTerminationCause.STREAM_ERROR);
                    return null;
                }
            } else {
                sessionContext.endSession(SessionTerminationCause.STREAM_ERROR);
                // TODO log warning
                return null;
            }
        } else {
            sessionContext.endSession(SessionTerminationCause.STREAM_ERROR);
            return null;
        }
    }
}
