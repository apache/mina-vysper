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

package org.apache.vysper.xmpp.modules.core.base.handler;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import java.util.Collections;
import java.util.List;

public class TestIQHandler extends IQHandler {

    String name = null;

    private IQStanza incomingStanza;

    private String namespaceURI;

    public TestIQHandler() {
        // empty
    }

    public TestIQHandler(String name, String namespaceURI) {
        this.name = name;
        this.namespaceURI = namespaceURI;
    }

    @Override
    public boolean verify(Stanza stanza) {
        if (!super.verify(stanza))
            return false;
        if (name == null)
            return true;
        return stanza.getVerifier().onlySubelementEquals(name, namespaceURI);
    }

    @Override
    protected List<Stanza> executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
										  SessionContext sessionContext, StanzaBroker stanzaBroker) {
        incomingStanza = stanza;

        StanzaBuilder responseBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT, stanza
                .getNamespacePrefix());
        if (stanza.getID() != null)
            responseBuilder.addAttribute("id", stanza.getID());

        responseBuilder.addAttribute("type", IQStanzaType.RESULT.value());

        return Collections.singletonList(responseBuilder.build());
    }

    public IQStanza getIncomingStanza() {
        return incomingStanza;
    }
}
