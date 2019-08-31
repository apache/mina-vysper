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
package org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.handler;

import static org.mockito.Mockito.mock;

import org.apache.vysper.StanzaAssert;
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

import junit.framework.Assert;

/**
 */
public class AuthCompatibilityIQHandlerTestCase {

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = mock(SessionContext.class);

    private IQStanza stanzaWithSet = (IQStanza) IQStanza.getWrapper(buildStanza("set"));

    private IQStanza stanzaWithGet = (IQStanza) IQStanza.getWrapper(buildStanza("get"));

    private IQStanza stanzaWithResult = (IQStanza) IQStanza.getWrapper(buildStanza("result"));

    private IQStanza stanzaWithError = (IQStanza) IQStanza.getWrapper(buildStanza("error"));

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private AuthCompatibilityIQHandler handler = new AuthCompatibilityIQHandler();

    private RecordingStanzaBroker stanzaBroker;

    private Stanza buildStanza(String type) {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", NamespaceURIs.JABBER_IQ_AUTH_COMPATIBILITY,
                type);
    }

    private Stanza buildStanza(String name, String namespaceUri, String type) {
        return buildStanza(name, namespaceUri, "query", NamespaceURIs.JABBER_IQ_AUTH_COMPATIBILITY, type);
    }

    private Stanza buildStanza(String name, String namespaceUri, String innerName, String innerNamespaceUri,
            String type) {
        return new StanzaBuilder(name, namespaceUri).addAttribute("type", type).addAttribute("id", "1")
                .startInnerElement(innerName, innerNamespaceUri).build();
    }

    @Before
    public void before() {
        stanzaBroker = new RecordingStanzaBroker();
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
        Assert.assertFalse(handler.verify(buildStanza("dummy", NamespaceURIs.JABBER_CLIENT, "set")));
    }

    @Test
    public void verifyInvalidNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", "dummy", "set")));
    }

    @Test
    public void verifyNullNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", null, "set")));
    }

    @Test
    public void verifyNullInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", null, "set")));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", "dummy", "set")));
    }

    @Test
    public void verifyMissingInnerElement() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Assert.assertTrue(handler.verify(stanzaWithSet));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void handleSet() throws BindException {
        assertResponse(stanzaWithSet);
    }

    @Test
    public void handleGet() throws BindException {
        assertResponse(stanzaWithGet);
    }

    @Test
    public void handleResult() throws BindException {
        handler.execute(stanzaWithResult, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                stanzaBroker);

        Assert.assertFalse(stanzaBroker.hasStanzaWrittenToSession());
    }

    @Test
    public void handleError() throws BindException {
        handler.execute(stanzaWithError, serverRuntimeContext, false, sessionContext, sessionStateHolder, stanzaBroker);

        Assert.assertFalse(stanzaBroker.hasStanzaWrittenToSession());
    }

    private void assertResponse(Stanza stanza) {
        handler.execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expectedResponse = StanzaBuilder.createIQStanza(null, null, IQStanzaType.ERROR, "1")
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_AUTH_COMPATIBILITY).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "cancel")
                .startInnerElement("service-unavailable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
                .endInnerElement().startInnerElement("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
                .addAttribute(NamespaceURIs.XML, "lang", "en").addText("jabber:iq:auth not supported").build();

        StanzaAssert.assertEquals(expectedResponse, stanzaBroker.getUniqueStanzaWrittenToSession());
    }

}
