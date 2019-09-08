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

import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandlerBaseTestCase;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 */
public class RelayingIQHandlerTestCase extends PresenceHandlerBaseTestCase {

    protected RelayingIQHandler relayingIQHandler = new RelayingIQHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        stanzaBroker = new RecordingStanzaBroker(new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
    }

    public void testIQClientToClient_Outbound_NotSubscribed() {
        Stanza iqStanza = StanzaBuilder
                .createIQStanza(initiatingUser.getEntityFQ(), unrelatedUser.getEntityFQ(), IQStanzaType.GET, "test")
                .startInnerElement("mandatory", NamespaceURIs.JABBER_CLIENT).build();

        relayingIQHandler.execute(iqStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker/* don't we have as sessionStateHolder? */);
        XMPPCoreStanza response = XMPPCoreStanza.getWrapper(stanzaBroker.getUniqueStanzaWrittenToSession());
        assertNotNull(response);
        assertTrue(response.isError());
    }

    public void testIQClientToClient_Outbound() {
        Stanza iqStanza = StanzaBuilder
                .createIQStanza(initiatingUser.getEntityFQ(), subscribed_FROM.getEntityFQ(), IQStanzaType.GET, "test")
                .startInnerElement("mandatory", NamespaceURIs.JABBER_CLIENT).build();

        relayingIQHandler.execute(iqStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker /* don't we have as sessionStateHolder? */);
        assertFalse(stanzaBroker.hasStanzaWrittenToSession());
        Stanza deliveredStanza = subscribed_FROM.getNextStanza();
        assertTrue(deliveredStanza.getVerifier().onlySubelementEquals("mandatory", NamespaceURIs.JABBER_CLIENT));
        assertEquals(subscribed_FROM.getEntityFQ(), deliveredStanza.getTo());
    }

    public void testIQClientToClient_Inbound_NoTO() {
        Stanza iqStanza = StanzaBuilder
                .createIQStanza(subscribed_FROM.getEntityFQ(), initiatingUser.getEntityFQ(), IQStanzaType.GET, "test")
                .startInnerElement("mandatory", NamespaceURIs.JABBER_CLIENT).build();

        relayingIQHandler.execute(iqStanza, sessionContext.getServerRuntimeContext(), false, sessionContext, null,
                stanzaBroker/* don't we have as sessionStateHolder? */);
        XMPPCoreStanza response = XMPPCoreStanza.getWrapper(stanzaBroker.getUniqueStanzaWrittenToSession());
        assertNotNull(response);
        assertTrue(response.isError());
    }

    public void testIQClientToClient_Inbound() {
        Stanza iqStanza = StanzaBuilder
                .createIQStanza(subscribed_TO.getEntityFQ(), initiatingUser.getEntityFQ(), IQStanzaType.GET, "test")
                .startInnerElement("mandatory", NamespaceURIs.JABBER_CLIENT).build();

        relayingIQHandler.execute(iqStanza, sessionContext.getServerRuntimeContext(), false, sessionContext, null,
                stanzaBroker/* don't we have as sessionStateHolder? */);
        Stanza deliveredStanza = stanzaBroker.getUniqueStanzaWrittenToSession();
        assertNotNull(deliveredStanza);
        assertTrue(deliveredStanza.getVerifier().onlySubelementEquals("mandatory", NamespaceURIs.JABBER_CLIENT));
        assertEquals(initiatingUser.getEntityFQ(), deliveredStanza.getTo());
    }
}
