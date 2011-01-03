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
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnector;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DbResultHandler implements StanzaHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(DbResultHandler.class);

    
    public String getName() {
        return "result";
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
        return false;
    }

    public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {

        String type = stanza.getAttributeValue("type");
        
        if(type == null) {
            // acting as the Receiving server
            // start of dailback, respond
            String streamId = sessionContext.getSessionId();
            String dailbackId = stanza.getInnerText().getText();
            Entity receiving = EntityImpl.parseUnchecked(stanza.getAttributeValue("from"));
            Entity originating = serverRuntimeContext.getServerEnitity();
            
            try {
                XMPPServerConnector connector = serverRuntimeContext.getServerConnectorRegistry().connectForDialback(receiving, sessionContext, sessionStateHolder);
                
                /*
                     <db:verify
                      from='target.tld'
                      id='417GAF25'
                      to='sender.tld'>
                       38b501ec606752318f72ad53de17ac6d15f86257485b0d8f5d54e1f619e6b869
                     </db:verify>
                 */
                
                StanzaBuilder verifyBuilder = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK, "db");
                verifyBuilder.addAttribute("from", originating.getFullQualifiedName());
                verifyBuilder.addAttribute("to", receiving.getFullQualifiedName());
                verifyBuilder.addAttribute("id", sessionContext.getSessionId());
                verifyBuilder.addText(dailbackId);
                connector.write(verifyBuilder.build());
                return null;
            } catch (Exception e) {
                StanzaBuilder builder = new StanzaBuilder("result", NamespaceURIs.JABBER_SERVER_DIALBACK, "db");
                builder.addAttribute("from", originating.getDomain());
                builder.addAttribute("to", receiving.getDomain());
                builder.addAttribute("type", "invalid");
                return new ResponseStanzaContainerImpl(builder.build());
            }
        } else {
            // acting as the Originating server
            // receiving the result from the Receiving server
            if("valid".equals(type)) {
                sessionStateHolder.setState(SessionState.AUTHENTICATED);
                Entity receiving = EntityImpl.parseUnchecked(stanza.getAttributeValue("from"));
                LOG.info("XMPP server connector to {} authenticated using dialback", receiving);
            } 

            return null;
        }
    }
}
