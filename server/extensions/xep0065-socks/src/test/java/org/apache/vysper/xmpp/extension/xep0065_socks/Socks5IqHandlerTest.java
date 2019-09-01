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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.vysper.xml.fragment.XMLSemanticError;
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
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

/**
 */
public class Socks5IqHandlerTest extends Mockito {

    private static final Entity FROM = EntityImpl.parseUnchecked("requestor@vysper.org");

    private static final Entity TARGET = EntityImpl.parseUnchecked("target@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("socks.vysper.org");

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private IQStanza stanza = (IQStanza) IQStanza.getWrapper(buildStanza());

    private Socks5ConnectionsRegistry connectionsRegistry = mock(Socks5ConnectionsRegistry.class);

    private Entity jid = EntityImpl.parseUnchecked("socks.vysper.org");

    private InetSocketAddress proxyAddress = new InetSocketAddress("1.2.3.4", 12345);

    private Socks5IqHandler handler = new Socks5IqHandler(jid, proxyAddress, connectionsRegistry);

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
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
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", "dummy")));
    }

    @Test
    public void verifyInvalidInnerName() {
        Assert.assertFalse(handler.verify(
                buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)));
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
    public void handleGet() throws BindException, XMLSemanticError {
        List<Stanza> responses = handler.handleGet(stanza, serverRuntimeContext, sessionContext, null);
        Stanza response = responses.get(0);

        Stanza expected = StanzaBuilder
                .createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .startInnerElement("query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
                .startInnerElement("streamhost", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
                .addAttribute("host", proxyAddress.getHostName()).addAttribute("jid", jid.getFullQualifiedName())
                .addAttribute("port", Integer.toString(proxyAddress.getPort())).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleGetDefaultAddress() throws BindException, XMLSemanticError {
        proxyAddress = new InetSocketAddress(12345);
        handler = new Socks5IqHandler(jid, proxyAddress, connectionsRegistry);
        List<Stanza> responses = handler.handleGet(stanza, serverRuntimeContext, sessionContext, null);
        Stanza response = responses.get(0);

        Stanza expected = StanzaBuilder
                .createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .startInnerElement("query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
                .startInnerElement("streamhost", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
                .addAttribute("host", jid.getFullQualifiedName()).addAttribute("jid", jid.getFullQualifiedName())
                .addAttribute("port", Integer.toString(proxyAddress.getPort())).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleSetActivate() throws BindException, XMLSemanticError {
        IQStanza request = (IQStanza) IQStanza.getWrapper(StanzaBuilder
                .createIQStanza(FROM, TO, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS).addAttribute("sid", "sid1")
                .startInnerElement("activate", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
                .addText(TARGET.getFullQualifiedName()).build());

        String hash = DigestUtils.shaHex("sid1" + FROM.getFullQualifiedName() + TARGET.getFullQualifiedName());
        when(connectionsRegistry.activate(hash)).thenReturn(true);

        List<Stanza> responses = handler.handleSet(request, serverRuntimeContext, sessionContext, null);
        Stanza response = responses.get(0);

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.RESULT, "id1").build();

        StanzaAssert.assertEquals(expected, response);

        verify(connectionsRegistry).activate(hash);
    }

}
