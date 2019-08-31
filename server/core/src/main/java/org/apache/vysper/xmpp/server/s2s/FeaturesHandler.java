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

package org.apache.vysper.xmpp.server.s2s;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DialbackIdGenerator;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class FeaturesHandler implements StanzaHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeaturesHandler.class);

    public String getName() {
        return "features";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        String namespaceURI = stanza.getNamespaceURI();
        return namespaceURI.equals(NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS);
    }

    public boolean isSessionRequired() {
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {
        if (sessionStateHolder.getState() != SessionState.AUTHENTICATED) {
            Entity otherServer = sessionContext.getInitiatingEntity();

            if (startTlsSupported(stanza) && serverRuntimeContext.getSslContext() != null) {
                // remote server support TLS and we got keys set up
                LOG.info("XMPP server connector to {} is starting TLS", otherServer);
                Stanza startTlsStanza = new StanzaBuilder("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS)
                        .build();

                stanzaBroker.writeToSession(startTlsStanza);
            } else if (dialbackSupported(stanza)) {
                Entity originating = serverRuntimeContext.getServerEntity();

                String dailbackId = new DialbackIdGenerator().generate(otherServer, originating,
                        sessionContext.getSessionId());

                Stanza dbResult = new StanzaBuilder("result", NamespaceURIs.JABBER_SERVER_DIALBACK, "db")
                        .addAttribute("from", originating.getDomain()).addAttribute("to", otherServer.getDomain())
                        .addText(dailbackId).build();

                stanzaBroker.writeToSession(dbResult);
            } else {
                // TODO how to handle
                throw new RuntimeException("Unsupported features");
            }
        }
    }

    private boolean startTlsSupported(Stanza stanza) {
        return !stanza.getInnerElementsNamed("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).isEmpty();
    }

    private boolean dialbackSupported(Stanza stanza) {
        return !stanza.getInnerElementsNamed("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).isEmpty();
    }

}
