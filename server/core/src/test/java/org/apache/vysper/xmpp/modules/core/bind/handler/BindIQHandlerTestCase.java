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
package org.apache.vysper.xmpp.modules.core.bind.handler;

import java.util.List;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
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
public class BindIQHandlerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private IQStanza stanza = (IQStanza) IQStanza.getWrapper(buildStanza());

    private BindIQHandler handler = new BindIQHandler();

    @Before
    public void before() throws BindException {
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
    }

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "bind", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "bind", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND);
    }

    private Stanza buildStanza(String name, String namespaceUri, String innerName, String innerNamespaceUri) {
        return new StanzaBuilder(name, namespaceUri).addAttribute("id", "1")
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
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "bind", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "bind", "dummy")));
    }

    @Test
    public void verifyInvalidInnerName() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy",
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND)));
    }

    @Test
    public void verifyMissingInnerElement() {
        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).build();
        Assert.assertFalse(handler.verify(stanza));
    }

    @Test
    public void verifyValidStanza() {
        Assert.assertTrue(handler.verify(stanza));
    }

    @Test
    public void sessionIsRequired() {
        Assert.assertTrue(handler.isSessionRequired());
    }

    @Test
    public void handleSet() throws BindException {
        Mockito.when(sessionContext.bindResource()).thenReturn("res");

        List<Stanza> responses = handler.handleSet(stanza, serverRuntimeContext, sessionContext, null);

        Stanza expectedResponse = StanzaBuilder.createIQStanza(null, null, IQStanzaType.RESULT, stanza.getID())
                .startInnerElement("bind", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND)
                .startInnerElement("jid", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND)
                .addText(new EntityImpl(FROM, "res").getFullQualifiedName()).build();

        StanzaAssert.assertEquals(expectedResponse, responses.get(0));
    }

    @Test
    public void handleSetWithBindException() throws BindException {
        Mockito.when(sessionContext.bindResource()).thenThrow(new BindException());

        List<Stanza> responses = handler.handleSet(stanza, serverRuntimeContext, sessionContext, null);

        Stanza expectedResponse = StanzaBuilder.createIQStanza(null, null, IQStanzaType.ERROR, stanza.getID())
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "cancel")
                .startInnerElement("not-allowed", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expectedResponse, responses.get(0));
    }

}
