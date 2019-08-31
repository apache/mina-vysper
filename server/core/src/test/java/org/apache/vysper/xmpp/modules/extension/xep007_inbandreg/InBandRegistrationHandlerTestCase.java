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
package org.apache.vysper.xmpp.modules.extension.xep007_inbandreg;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg.InBandRegistrationHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class InBandRegistrationHandlerTestCase {

    private static final String IQ_ID = "id1";

    private AccountManagement accountManagement = Mockito.mock(AccountManagement.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionStateHolder sessionStateHolder = Mockito.mock(SessionStateHolder.class);

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");

    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");

    private static final Entity EXISTING = EntityImpl.parseUnchecked("existing@vysper.org");

    protected InBandRegistrationHandler handler = new InBandRegistrationHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Before
    public void before() {
        Mockito.when(serverRuntimeContext.getStorageProvider(AccountManagement.class)).thenReturn(accountManagement);
        Mockito.when(serverRuntimeContext.getServerEntity()).thenReturn(SERVER);
        Mockito.when(accountManagement.verifyAccountExists(EXISTING)).thenReturn(true);
        Mockito.when(sessionContext.getState()).thenReturn(SessionState.STARTED);
        stanzaBroker = new RecordingStanzaBroker();
    }

    @Test
    public void testGetUnauthenticated() throws XMLSemanticError {
        // <iq type='get' id='reg1'>
        // <query xmlns='jabber:iq:register'/>
        // </iq>
        Stanza get = StanzaBuilder.createIQStanza(FROM, SERVER, IQStanzaType.GET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER).build();

        handler.execute(get, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        // <iq type='result' id='reg1'>
        // <query xmlns='jabber:iq:register'>
        // <instructions>
        // Choose a username and password for use with this service.
        // Please also provide your email address.
        // </instructions>
        // <username/>
        // <password/>
        // <email/>
        // </query>
        // </iq>
        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(FROM.getFullQualifiedName(), response.getAttributeValue("to"));

        XMLElement query = response.getSingleInnerElementsNamed("query", NamespaceURIs.JABBER_IQ_REGISTER);
        Assert.assertNotNull(query.getSingleInnerElementsNamed("instructions", NamespaceURIs.JABBER_IQ_REGISTER));
        Assert.assertNotNull(query.getSingleInnerElementsNamed("username", NamespaceURIs.JABBER_IQ_REGISTER));
        Assert.assertNotNull(query.getSingleInnerElementsNamed("password", NamespaceURIs.JABBER_IQ_REGISTER));
    }

    @Test
    public void testGetAuthenticated() throws XMLSemanticError {
        // <iq type='get' id='reg1'>
        // <query xmlns='jabber:iq:register'/>
        // </iq>
        Stanza get = StanzaBuilder.createIQStanza(EXISTING, SERVER, IQStanzaType.GET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER).build();

        Mockito.when(sessionContext.getState()).thenReturn(SessionState.AUTHENTICATED);
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(EXISTING);

        handler.execute(get, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        // <iq type='result' id='reg1'>
        // <query xmlns='jabber:iq:register'>
        // <registered/>
        // <username>juliet</username>
        // <password>R0m30</password>
        // <email>juliet@capulet.com</email>
        // </query>
        // </iq>
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals("result", response.getAttributeValue("type"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(EXISTING.getFullQualifiedName(), response.getAttributeValue("to"));

        XMLElement query = response.getSingleInnerElementsNamed("query", NamespaceURIs.JABBER_IQ_REGISTER);
        Assert.assertNotNull(query.getSingleInnerElementsNamed("registered", NamespaceURIs.JABBER_IQ_REGISTER));
        assertInnerText(EXISTING.getNode(), "username", query);
    }

    @Test
    public void testSet() throws XMLSemanticError, AccountCreationException {
        // <iq type='set' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>Calliope</password>
        // <email>bard@shakespeare.lit</email>
        // </query>
        // </iq>
        Stanza set = StanzaBuilder.createIQStanza(FROM, SERVER, IQStanzaType.SET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER)
                .startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER).addText(FROM.getNode())
                .endInnerElement().startInnerElement("password", NamespaceURIs.JABBER_IQ_REGISTER).addText("password")
                .endInnerElement().endInnerElement().build();

        handler.execute(set, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        // <iq type='result' id='reg2'/>
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals("result", response.getAttributeValue("type"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(FROM.getFullQualifiedName(), response.getAttributeValue("to"));
        Assert.assertEquals(0, response.getInnerElements().size());

        Mockito.verify(accountManagement).addUser(FROM, "password");
    }

    @Test
    public void testSetExisting() throws XMLSemanticError, AccountCreationException {
        // <iq type='set' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>Calliope</password>
        // <email>bard@shakespeare.lit</email>
        // </query>
        // </iq>
        Stanza set = StanzaBuilder.createIQStanza(EXISTING, SERVER, IQStanzaType.SET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER)
                .startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER).addText(EXISTING.getNode())
                .endInnerElement().startInnerElement("password", NamespaceURIs.JABBER_IQ_REGISTER).addText("password")
                .endInnerElement().endInnerElement().build();

        handler.execute(set, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        // <iq type='error' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>m1cro$oft</password>
        // <email>billg@bigcompany.com</email>
        // </query>
        // <error code='409' type='cancel'>
        // <conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
        // </error>
        // </iq>
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals("error", response.getAttributeValue("type"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(EXISTING.getFullQualifiedName(), response.getAttributeValue("to"));

        XMLElement query = response.getSingleInnerElementsNamed("query", NamespaceURIs.JABBER_IQ_REGISTER);
        assertInnerText(EXISTING.getNode(), "username", query);
        assertInnerText("password", "password", query);

        XMLElement error = response.getSingleInnerElementsNamed("error", NamespaceURIs.JABBER_CLIENT);
        Assert.assertEquals("409", error.getAttributeValue("code"));
        Assert.assertEquals("cancel", error.getAttributeValue("type"));
        Assert.assertNotNull(
                error.getSingleInnerElementsNamed("conflict", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS));

        Mockito.verify(accountManagement, Mockito.never()).addUser(EXISTING, "password");
    }

    @Test
    public void testSetMissingPassword() throws XMLSemanticError, AccountCreationException {
        // <iq type='set' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>Calliope</password>
        // <email>bard@shakespeare.lit</email>
        // </query>
        // </iq>
        Stanza set = StanzaBuilder.createIQStanza(FROM, SERVER, IQStanzaType.SET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER)
                .startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER).addText(FROM.getNode())
                .endInnerElement().endInnerElement().build();

        handler.execute(set, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        // <iq type='error' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>Calliope</password>
        // </query>
        // <error code='406' type='modify'>
        // <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
        // </error>
        // </iq>
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals("error", response.getAttributeValue("type"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(FROM.getFullQualifiedName(), response.getAttributeValue("to"));

        XMLElement query = response.getSingleInnerElementsNamed("query", NamespaceURIs.JABBER_IQ_REGISTER);
        assertInnerText(FROM.getNode(), "username", query);

        XMLElement error = response.getSingleInnerElementsNamed("error", NamespaceURIs.JABBER_CLIENT);
        Assert.assertEquals("406", error.getAttributeValue("code"));
        Assert.assertEquals("modify", error.getAttributeValue("type"));
        Assert.assertNotNull(
                error.getSingleInnerElementsNamed("not-acceptable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS));

        Mockito.verify(accountManagement, Mockito.never()).addUser(EXISTING, "password");
    }

    @Test
    public void testSetChangePassword() throws XMLSemanticError, AccountCreationException {
        // <iq type='set' id='reg2'>
        // <query xmlns='jabber:iq:register'>
        // <username>bill</username>
        // <password>Calliope</password>
        // <email>bard@shakespeare.lit</email>
        // </query>
        // </iq>
        Stanza set = StanzaBuilder.createIQStanza(EXISTING, SERVER, IQStanzaType.SET, IQ_ID)
                .startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER)
                .startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER).addText(EXISTING.getNode())
                .endInnerElement().startInnerElement("password", NamespaceURIs.JABBER_IQ_REGISTER).addText("password")
                .endInnerElement().endInnerElement().build();

        Mockito.when(sessionContext.getState()).thenReturn(SessionState.AUTHENTICATED);
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(EXISTING);

        handler.execute(set, serverRuntimeContext, true, sessionContext, sessionStateHolder, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        // <iq type='result' id='reg2'/>
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals(IQ_ID, response.getAttributeValue("id"));
        Assert.assertEquals("result", response.getAttributeValue("type"));
        Assert.assertEquals(SERVER.getFullQualifiedName(), response.getAttributeValue("from"));
        Assert.assertEquals(EXISTING.getFullQualifiedName(), response.getAttributeValue("to"));
        Assert.assertEquals(0, response.getInnerElements().size());

        Mockito.verify(accountManagement).changePassword(EXISTING, "password");
    }

    private void assertInnerText(String expected, String name, XMLElement parent) {
        try {
            Assert.assertEquals(expected, parent.getSingleInnerElementsNamed(name, NamespaceURIs.JABBER_IQ_REGISTER)
                    .getInnerText().getText());
        } catch (XMLSemanticError e) {
            Assert.fail("Incorrect number of elements: " + e.getMessage());
        }

    }

}
