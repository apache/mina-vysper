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
package org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
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
public class VcardTempIQHandlerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");

    private static final Entity OTHER = EntityImpl.parseUnchecked("other@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private StanzaBroker stanzaBroker = mock(StanzaBroker.class);

    private VcardTempPersistenceManager persistenceManager = mock(VcardTempPersistenceManager.class);

    private IQStanza verifyStanza = (IQStanza) IQStanza.getWrapper(buildStanza());

    private VcardTempIQHandler handler = new VcardTempIQHandler();

    private static final XMLElement VCARD = new XMLElementBuilder("vCard", NamespaceURIs.VCARD_TEMP)
            .startInnerElement("FN", NamespaceURIs.VCARD_TEMP).addText("JeremieMiller").endInnerElement()
            .startInnerElement("NICKNAME", NamespaceURIs.VCARD_TEMP).addText("jer").endInnerElement().build();

    private static final XMLElement OTHER_VCARD = new XMLElementBuilder("vCard", NamespaceURIs.VCARD_TEMP)
            .startInnerElement("FN", NamespaceURIs.VCARD_TEMP).addText("DonaldDuck").endInnerElement()
            .startInnerElement("NICKNAME", NamespaceURIs.VCARD_TEMP).addText("don").endInnerElement().build();

    private static final String VCARD_STRING = new Renderer(VCARD).getComplete();

    private static final String OTHER_VCARD_STRING = new Renderer(OTHER_VCARD).getComplete();

    @Before
    public void before() {
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);

        Mockito.when(persistenceManager.getVcard(FROM)).thenReturn(VCARD_STRING);
        Mockito.when(persistenceManager.getVcard(OTHER)).thenReturn(OTHER_VCARD_STRING);

        handler.setPersistenceManager(persistenceManager);
    }

    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "vCard", NamespaceURIs.VCARD_TEMP);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "vCard", NamespaceURIs.VCARD_TEMP);
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
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "vCard", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "vCard", "dummy")));
    }

    @Test
    public void verifyInvalidInnerName() {
        Assert.assertFalse(
                handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy", NamespaceURIs.VCARD_TEMP)));
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
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1").addPreparedElement(VCARD)
                .build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.RESULT, "id1").addPreparedElement(VCARD)
                .build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleGetNonExisting() throws BindException, XMLSemanticError {
        Mockito.when(persistenceManager.getVcard(FROM)).thenReturn(null);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.RESULT, "id1")
                .startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleGetForOtherUser() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, OTHER, IQStanzaType.GET, "id1")
                .startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(OTHER, FROM, IQStanzaType.RESULT, "id1")
                .addPreparedElement(OTHER_VCARD).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleGetWithoutPersitenceManager() throws BindException, XMLSemanticError {
        handler.setPersistenceManager(null);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.GET, "id1")
                .startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "wait")
                .startInnerElement("internal-server-error", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleSet() throws BindException, XMLSemanticError {
        Mockito.when(persistenceManager.setVcard(FROM, VCARD_STRING)).thenReturn(true);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1").addPreparedElement(VCARD)
                .build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(null, FROM, IQStanzaType.RESULT, "id1").build();

        verify(stanzaBroker).writeToSession(expected);

        Mockito.verify(persistenceManager).setVcard(FROM, VCARD_STRING);
    }

    @Test
    public void handleSetWithoutPersistenceManager() throws BindException, XMLSemanticError {
        handler.setPersistenceManager(null);

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1").addPreparedElement(VCARD)
                .build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1").addPreparedElement(VCARD)
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "wait")
                .startInnerElement("internal-server-error", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleSetWithoutVCard() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1")
                .startInnerElement("dummy", NamespaceURIs.VCARD_TEMP).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .startInnerElement("dummy", NamespaceURIs.VCARD_TEMP).endInnerElement()
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "modify")
                .startInnerElement("bad-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleSetForOtherUser() throws BindException, XMLSemanticError {
        Stanza request = StanzaBuilder.createIQStanza(FROM, OTHER, IQStanzaType.SET, "id1")
                .addPreparedElement(OTHER_VCARD).build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(TO, FROM, IQStanzaType.ERROR, "id1")
                .addPreparedElement(OTHER_VCARD).startInnerElement("error", NamespaceURIs.JABBER_CLIENT)
                .addAttribute("type", "auth")
                .startInnerElement("forbidden", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).build();

        verify(stanzaBroker).writeToSession(expected);
    }

    @Test
    public void handleSetFailedPersisting() throws BindException, XMLSemanticError {
        // persistence manager mock will always default to return false

        Stanza request = StanzaBuilder.createIQStanza(FROM, FROM, IQStanzaType.SET, "id1").addPreparedElement(VCARD)
                .build();

        handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza expected = StanzaBuilder.createIQStanza(null, FROM, IQStanzaType.ERROR, "id1").build();

        verify(stanzaBroker).writeToSession(expected);
    }

}
