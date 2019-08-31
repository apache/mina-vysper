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
package org.apache.vysper.xmpp.modules.core.session.handler;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

/**
 */
public class SessionIQHandlerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private IQStanza verifyStanza = (IQStanza) IQStanza.getWrapper(buildStanza());

    private SessionIQHandler handler = new SessionIQHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() {
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
        Mockito.when(sessionContext.getServerJID()).thenReturn(TO);

        stanzaBroker = new RecordingStanzaBroker();
    }

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "session",
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "session", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION);
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
        Assert.assertFalse(handler.verify(null));
    }

    @Test
    public void verifyInvalidName() {
        Assert.assertFalse(handler.verify(buildStanza("dummy", NamespaceURIs.JABBER_CLIENT)));
    }

    @Test
    public void verifyInvalidNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", "dummy")));
    }

    @Test
    public void verifyNullNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", null)));
    }

    @Test
    public void verifyNullInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "session", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "session", "dummy")));
    }

    @Test
    public void verifyInvalidInnerName() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy",
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION)));
    }

    @Test
    public void verifyMissingInnerElement() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Assert.assertTrue(handler.verify(verifyStanza));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void handleSet() throws XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("session", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, null, IQStanzaType.RESULT, "id1").build();

        StanzaAssert.assertEquals(expected, response);
    }

}
