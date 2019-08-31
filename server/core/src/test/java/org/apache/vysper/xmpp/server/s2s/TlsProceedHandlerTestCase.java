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

import javax.net.ssl.SSLContext;

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
public class TlsProceedHandlerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private TlsProceedHandler handler = new TlsProceedHandler();

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() {
        SSLContext sslContext = Mockito.mock(SSLContext.class);
        Mockito.when(serverRuntimeContext.getSslContext()).thenReturn(sslContext);
        Mockito.when(serverRuntimeContext.getServerEntity()).thenReturn(TO);

        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
        Mockito.when(sessionContext.getSessionId()).thenReturn("session-id");

        sessionStateHolder.setState(SessionState.STARTED);

        stanzaBroker = new RecordingStanzaBroker();
    }

    @Test
    public void nameMustBeFeatures() {
        Assert.assertEquals("proceed", handler.getName());
    }

    @Test
    public void verifyNullStanza() {
        Assert.assertFalse(handler.verify(null));
    }

    @Test
    public void verifyInvalidName() {
        Stanza stanza = new StanzaBuilder("dummy", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyInvalidNamespace() {
        Stanza stanza = new StanzaBuilder("proceed", "dummy").build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyNullNamespace() {
        Stanza stanza = new StanzaBuilder("proceed").build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Stanza stanza = new StanzaBuilder("proceed", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).build();
        Assert.assertTrue(handler.verify(stanza));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void execute() {
        Stanza stanza = new StanzaBuilder("proceed", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS).build();

        handler.execute(stanza, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Assert.assertFalse(stanzaBroker.hasStanzaWrittenToSession());

        Assert.assertEquals(SessionState.ENCRYPTION_STARTED, sessionStateHolder.getState());

        Mockito.verify(sessionContext).switchToTLS(false, true);
    }

}
