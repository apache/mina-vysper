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

package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionContext.SessionTerminationCause;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DbVerifyHandler implements StanzaHandler {
    private DialbackIdGenerator dailbackIdGenerator = new DialbackIdGenerator();

    public String getName() {
        return "verify";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        String namespaceURI = stanza.getNamespaceURI();
        if (namespaceURI == null)
            return false;
        return namespaceURI.equals(NamespaceURIs.JABBER_SERVER_DIALBACK);
    }

    public boolean isSessionRequired() {
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {

        String type = stanza.getAttributeValue("type");
        String id = stanza.getAttributeValue("id");
        Entity receiving = EntityImpl.parseUnchecked(stanza.getAttributeValue("from"));
        Entity originating = serverRuntimeContext.getServerEntity();
        if (type == null) {
            // acting as a Authoritative server
            // getting asked for verification from the Receiving server
            String dailbackId = stanza.getInnerText().getText();

            StanzaBuilder builder = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK, "db");
            builder.addAttribute("from", originating.getDomain());
            builder.addAttribute("to", receiving.getDomain());
            builder.addAttribute("id", id);

            if (dailbackIdGenerator.verify(dailbackId, receiving, originating, id)) {
                builder.addAttribute("type", "valid");
            } else {
                builder.addAttribute("type", "invalid");
            }
            stanzaBroker.writeToSession(builder.build());
            return;
        } else {
            // acting as a Receiving server
            // getting a response from the Authoritative server
            SessionStateHolder dialbackSessionStateHolder = (SessionStateHolder) sessionContext
                    .getAttribute("DIALBACK_SESSION_STATE_HOLDER");
            SessionContext dialbackSessionContext = (SessionContext) sessionContext
                    .getAttribute("DIALBACK_SESSION_CONTEXT");

            // XMPPServerConnector connector =
            // serverRuntimeContext.getServerConnectorRegistry().getConnectorBySessionId(id);

            // if(connector != null) {
            // SessionStateHolder dialbackSessionStateHolder =
            // connector.getSessionStateHolder();
            // SessionContext dialbackSessionContext = connector.getSessionContext();

            Entity otherServer = sessionContext.getInitiatingEntity();
            String resultType = "invalid";
            // dialbackSessionContext must be non-null or someone is trying to send this
            // stanza in the wrong state
            if ("valid".equals(type)) {
                dialbackSessionStateHolder.setState(SessionState.AUTHENTICATED);
                dialbackSessionContext.setInitiatingEntity(otherServer);
                resultType = "valid";
            }

            // <db:result xmlns:db="jabber:server:dialback" to="xmpp.protocol7.com"
            // from="jabber.org" type="valid"></db:result>
            StanzaBuilder builder = new StanzaBuilder("result", NamespaceURIs.JABBER_SERVER_DIALBACK, "db");
            builder.addAttribute("from", originating.getDomain());
            builder.addAttribute("to", otherServer.getDomain());
            builder.addAttribute("type", resultType);

            stanzaBroker.writeToSession(builder.build());
            // }

            // close this session as we are now done checking dialback
            sessionContext.endSession(SessionTerminationCause.CLIENT_BYEBYE);
        }
    }
}
