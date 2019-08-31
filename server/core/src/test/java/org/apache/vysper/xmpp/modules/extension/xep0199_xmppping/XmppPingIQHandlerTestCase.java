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

import static org.junit.Assert.assertFalse;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class XmppPingIQHandlerTestCase {

    private static final String IQ_ID = "xmppping-1";

    private TestSessionContext sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();

    protected Entity client = EntityImpl.parseUnchecked("tester@vysper.org");

    protected Entity boundClient;

    protected Entity server;

    protected XmppPingIQHandler handler = new XmppPingIQHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() throws Exception {
        sessionContext.setInitiatingEntity(client);

        boundClient = new EntityImpl(client, sessionContext.bindResource());
        server = sessionContext.getServerJID();

        stanzaBroker = new RecordingStanzaBroker();
    }

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "ping", NamespaceURIs.URN_XMPP_PING);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "ping", NamespaceURIs.URN_XMPP_PING);
    }

    private Stanza buildStanza(String name, String namespaceUri, String innerName, String innerNamespaceUri) {
        return new StanzaBuilder(name, namespaceUri).addAttribute("type", "get").addAttribute("id", "1")
                .startInnerElement(innerName, innerNamespaceUri).build();
    }

    @Test
    public void nameMustBeIq() {
        Assert.assertEquals("iq", handler.getName());
    }

    @Test
    public void verifyNullStanza() {
        assertFalse(handler.verify(null));
    }

    @Test
    public void verifyInvalidName() {
        assertFalse(handler.verify(buildStanza("dummy", NamespaceURIs.JABBER_CLIENT)));
    }

    @Test
    public void verifyInvalidNamespace() {
        assertFalse(handler.verify(buildStanza("iq", "dummy")));
    }

    @Test
    public void verifyNullNamespace() {
        assertFalse(handler.verify(buildStanza("iq", null)));
    }

    @Test
    public void verifyNullInnerNamespace() {
        assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "ping", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "ping", "dummy")));
    }

    @Test
    public void verifyInvalidInnerName() {
        assertFalse(
                handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy", NamespaceURIs.URN_XMPP_PING)));
    }

    @Test
    public void verifyMissingInnerElement() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).build();
        assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Assert.assertTrue(handler.verify(buildStanza()));
    }

    @Test
    public void verifyValidResultStanza() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "result")
                .addAttribute("id", "xmppping-1").build();

        Assert.assertTrue(handler.verify(stanza));
    }

    @Test
    public void verifyInvalidResultStanza() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "result")
                .addAttribute("id", "dummy-1").build();

        assertFalse(handler.verify(stanza));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void clientToServerPing() {
        // C: <iq from='juliet@capulet.lit/balcony' to='capulet.lit' id='c2s1'
        // type='get'>
        // <ping xmlns='urn:xmpp:ping'/>
        // </iq>
        //
        // S: <iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='c2s1'
        // type='result'/>

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(boundClient, server, IQStanzaType.GET, IQ_ID);
        stanzaBuilder.startInnerElement("ping", NamespaceURIs.URN_XMPP_PING).endInnerElement();

        Stanza requestStanza = stanzaBuilder.build();
        handler.execute(requestStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker);

        // we should always get a response
        Assert.assertTrue(stanzaBroker.hasStanzaWrittenToSession());

        Stanza respStanza = stanzaBroker.getUniqueStanzaWrittenToSession();

        Assert.assertEquals("iq", respStanza.getName());
        Assert.assertEquals(boundClient, respStanza.getTo());
        Assert.assertEquals(server, respStanza.getFrom());
        Assert.assertEquals(IQ_ID, respStanza.getAttributeValue("id"));
        Assert.assertEquals("result", respStanza.getAttributeValue("type"));
    }

    @Test
    public void handleResult() {
        // S: <iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='c2s1'
        // type='result'/>

        Stanza stanza = StanzaBuilder.createIQStanza(boundClient, server, IQStanzaType.RESULT, IQ_ID).build();

        XmppPinger pinger1 = Mockito.mock(XmppPinger.class);
        XmppPinger pinger2 = Mockito.mock(XmppPinger.class);
        XmppPinger pinger3 = Mockito.mock(XmppPinger.class);

        handler.addPinger(pinger1);
        handler.addPinger(pinger2);
        handler.addPinger(pinger3);
        handler.removePinger(pinger3);

        handler.execute(stanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null, stanzaBroker);

        assertFalse(stanzaBroker.hasStanzaWrittenToSession());

        Mockito.verify(pinger1).pong(IQ_ID);
        Mockito.verify(pinger1).pong(IQ_ID);
        Mockito.verifyZeroInteractions(pinger3);
    }

}
