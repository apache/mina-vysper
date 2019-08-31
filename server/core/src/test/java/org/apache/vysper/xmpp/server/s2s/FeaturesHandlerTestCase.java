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

import static org.mockito.Mockito.mock;

import javax.net.ssl.SSLContext;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

/**
 */
public class FeaturesHandlerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private FeaturesHandler handler = new FeaturesHandler();

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() {
        SSLContext sslContext = mock(SSLContext.class);
        Mockito.when(serverRuntimeContext.getSslContext()).thenReturn(sslContext);
        Mockito.when(serverRuntimeContext.getServerEntity()).thenReturn(TO);

        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
        Mockito.when(sessionContext.getSessionId()).thenReturn("session-id");

        sessionStateHolder.setState(SessionState.STARTED);

        stanzaBroker = new RecordingStanzaBroker();
    }

    @Test
    public void nameMustBeFeatures() {
        Assert.assertEquals("features", handler.getName());
    }

    @Test
    public void verifyNullStanza() {
        Assert.assertFalse(handler.verify(null));
    }

    @Test
    public void verifyInvalidName() {
        Stanza stanza = new StanzaBuilder("dummy", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS).build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyInvalidNamespace() {
        Stanza stanza = new StanzaBuilder("features", "dummy").build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyNullNamespace() {
        Stanza stanza = new StanzaBuilder("features").build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS).build();
        Assert.assertTrue(handler.verify(stanza));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void executeWithSsl() {
        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).endInnerElement()
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement().build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expectedResponse = new StanzaBuilder("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).build();

        StanzaAssert.assertEquals(expectedResponse, stanzaBroker.getUniqueStanzaWrittenToSession());
    }

    @Test
    public void executeWithSslDisabled() {
        Mockito.when(serverRuntimeContext.getSslContext()).thenReturn(null);

        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).endInnerElement()
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement().build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        assertDialbackStanza(stanzaBroker.getUniqueStanzaWrittenToSession());
    }

    @Test
    public void executeDialback() {
        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement().build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        assertDialbackStanza(stanzaBroker.getUniqueStanzaWrittenToSession());
    }

    @Test
    public void executeWhenAuthenticated() {
        sessionStateHolder.setState(SessionState.AUTHENTICATED);

        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement().build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Assert.assertFalse(stanzaBroker.hasStanzaWrittenToSession());
    }

    // TODO Is this the correct behavior?
    @Test(expected = RuntimeException.class)
    public void executeWithUnknownFeatures() {
        Stanza stanza = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("dummy", "dummy").endInnerElement().build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);
    }

    private void assertDialbackStanza(Stanza responseStanza) {
        Assert.assertEquals("result", responseStanza.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_SERVER_DIALBACK, responseStanza.getNamespaceURI());
        Assert.assertEquals("db", responseStanza.getNamespacePrefix());
        Assert.assertEquals(TO.getFullQualifiedName(), responseStanza.getAttributeValue("from"));
        Assert.assertEquals(FROM.getFullQualifiedName(), responseStanza.getAttributeValue("to"));

        Assert.assertNotNull(responseStanza.getInnerText().getText());
    }

}
