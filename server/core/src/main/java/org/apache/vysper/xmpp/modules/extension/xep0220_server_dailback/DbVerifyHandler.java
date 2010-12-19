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
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DbVerifyHandler implements StanzaHandler {
    private DailbackIdGenerator dailbackIdGenerator = new DailbackIdGenerator();
    
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
        return false;
    }

    public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        String dailbackId = stanza.getInnerText().getText();
        Entity receiving = EntityImpl.parseUnchecked(stanza.getAttributeValue("from"));
        Entity originating = serverRuntimeContext.getServerEnitity();
        String streamId = stanza.getAttributeValue("id");
        
        StanzaBuilder builder = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK);
        builder.addAttribute("from", originating.getDomain());
        builder.addAttribute("to", receiving.getDomain());
        builder.addAttribute("id", streamId);
        
        if(dailbackIdGenerator.verify(dailbackId, receiving, originating, streamId)) {
            builder.addAttribute("type", "valid");
        } else {
            builder.addAttribute("type", "invalid");
        }

        return new ResponseStanzaContainerImpl(builder.build());
    }
}
