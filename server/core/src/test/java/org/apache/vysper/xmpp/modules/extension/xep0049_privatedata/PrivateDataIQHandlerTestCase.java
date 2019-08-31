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
package org.apache.vysper.xmpp.modules.extension.xep0049_privatedata;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
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
public class PrivateDataIQHandlerTestCase {

    private static final String NS = "http://example.com";

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private static final String KEY = "foo-http-//example.com";

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private PrivateDataPersistenceManager persistenceManager = Mockito.mock(PrivateDataPersistenceManager.class);

    private IQStanza verifyStanza = (IQStanza) IQStanza.getWrapper(buildStanza());

    private PrivateDataIQHandler handler = new PrivateDataIQHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() {
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);

        Mockito.when(persistenceManager.getPrivateData(FROM, KEY)).thenReturn("<bar xmlns=\"http://example.com\" />");

        handler.setPersistenceManager(persistenceManager);

        stanzaBroker = new RecordingStanzaBroker();
    }

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "query", NamespaceURIs.PRIVATE_DATA);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "query", NamespaceURIs.PRIVATE_DATA);
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
        Assert.assertFalse(
                handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy", NamespaceURIs.PRIVATE_DATA)));
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
    public void handleGet() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.RESULT, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("bar", NS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleGetNonExisting() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("dummy", NS)
                .addAttribute("attr", "attrval").build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.RESULT, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("dummy", NS)
                .addAttribute("attr", "attrval").build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleGetWithoutInnerElement() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "modify")
                .startInnerElement("not-acceptable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleGetForOtherUser() throws BindException, XMLSemanticError {
        Entity other = EntityImpl.parseUnchecked("other@vysper.org");

        Stanza request = StanzaBuilder.createIQStanza(FROM, other, IQStanzaType.GET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "cancel")
                .addAttribute("code", "403")
                .startInnerElement("forbidden", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleGetWithoutPersitenceManager() throws BindException, XMLSemanticError {
        handler.setPersistenceManager(null);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).endInnerElement()
                .endInnerElement().startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "wait")
                .startInnerElement("internal-server-error", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleSet() throws BindException, XMLSemanticError {
        XMLElement stored = new XMLElementBuilder("foo", NS).startInnerElement("fez", NS).build();

        String storedXml = new Renderer(stored).getComplete();

        Mockito.when(persistenceManager.setPrivateData(FROM, KEY, storedXml)).thenReturn(true);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).addPreparedElement(stored).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(null, FROM, IQStanzaType.RESULT, "id1").build();

        StanzaAssert.assertEquals(expected, response);

        Mockito.verify(persistenceManager).setPrivateData(FROM, KEY, storedXml);
    }

    @Test
    public void handleSetWithoutPersistenceManager() throws BindException, XMLSemanticError {
        handler.setPersistenceManager(null);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).endInnerElement()
                .endInnerElement().startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "wait")
                .startInnerElement("internal-server-error", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleSetForOtherUser() throws BindException, XMLSemanticError {
        Entity other = EntityImpl.parseUnchecked("other@vysper.org");

        Stanza request = StanzaBuilder.createIQStanza(FROM, other, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).endInnerElement()
                .endInnerElement().startInnerElement("error", NamespaceURIs.JABBER_CLIENT)
                .addAttribute("type", "cancel").addAttribute("code", "403")
                .startInnerElement("forbidden", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleSetWithoutInnerElement() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "modify")
                .startInnerElement("not-acceptable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        StanzaAssert.assertEquals(expected, response);
    }

    @Test
    public void handleSetFailedPersisting() throws BindException, XMLSemanticError {
        // persistence manager mock will always default to return false

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("query", NamespaceURIs.PRIVATE_DATA).startInnerElement("foo", NS).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Stanza expected = StanzaBuilder.createIQStanza(null, FROM, IQStanzaType.ERROR, "id1").build();

        StanzaAssert.assertEquals(expected, response);
    }

}
