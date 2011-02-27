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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class XmppPingIQHandlerTestCase extends TestCase {

    private static final String IQ_ID = "id1";

    private TestSessionContext sessionContext;

    protected Entity client;

    protected Entity boundClient;

    protected Entity server;

    protected XmppPingIQHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        client = EntityImpl.parse("tester@vysper.org");

        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext.setInitiatingEntity(client);

        boundClient = new EntityImpl(client, sessionContext.bindResource());
        server = sessionContext.getServerJID();

        handler = new XmppPingIQHandler();
    }

    public void testClientToServerPing() {
        // C: <iq from='juliet@capulet.lit/balcony' to='capulet.lit' id='c2s1' type='get'>
        //      <ping xmlns='urn:xmpp:ping'/>
        //    </iq>
        //
        // S: <iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='c2s1' type='result'/>

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(boundClient, server, IQStanzaType.GET, IQ_ID);
        stanzaBuilder.startInnerElement("ping", NamespaceURIs.URN_XMPP_PING).endInnerElement();

        Stanza requestStanza = stanzaBuilder.build();
        ResponseStanzaContainer resp = handler.execute(requestStanza, sessionContext.getServerRuntimeContext(), true,
                sessionContext, null);

        // we should always get a response
        assertTrue(resp.hasResponse());

        Stanza respStanza = resp.getResponseStanza();

        assertEquals("iq", respStanza.getName());
        assertEquals(boundClient, respStanza.getTo());
        assertEquals(server, respStanza.getFrom());
        assertEquals(IQ_ID, respStanza.getAttributeValue("id"));
        assertEquals("result", respStanza.getAttributeValue("type"));
    }
}
