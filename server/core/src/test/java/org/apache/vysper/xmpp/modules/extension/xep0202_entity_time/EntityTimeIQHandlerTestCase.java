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
package org.apache.vysper.xmpp.modules.extension.xep0202_entity_time;

import java.util.List;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class EntityTimeIQHandlerTestCase {

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
    private SessionContext sessionContext = Mockito.mock(SessionContext.class);
    private IQStanza stanza = (IQStanza) IQStanza.getWrapper(buildStanza());
    
    private EntityTimeIQHandler handler = new EntityTimeIQHandler();
    
    private Stanza buildStanza() {
        return buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "time", NamespaceURIs.URN_XMPP_TIME);
    }

    private Stanza buildStanza(String name, String namespaceUri) {
        return buildStanza(name, namespaceUri, "time", NamespaceURIs.URN_XMPP_TIME);
    }
    
    private Stanza buildStanza(String name, String namespaceUri, String innerName, String innerNamespaceUri) {
        return new StanzaBuilder(name, namespaceUri)
            .addAttribute("type", "get")
            .addAttribute("id", "1")
            .startInnerElement(innerName, innerNamespaceUri)
            .build();
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
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "time", null)));
    }

    @Test
    public void verifyInvalidInnerNamespace() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "time", "dummy")));
    }
    
    @Test
    public void verifyInvalidInnerName() {
        Assert.assertFalse(handler.verify(buildStanza("iq", NamespaceURIs.JABBER_CLIENT, "dummy", NamespaceURIs.URN_XMPP_TIME)));
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
        
        Assert.assertNotNull(response);
        Assert.assertEquals("iq", response.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_CLIENT, response.getNamespaceURI());
        Assert.assertEquals("result", response.getAttributeValue("type"));
        Assert.assertEquals("1", response.getAttributeValue("id"));
        
        XMLElement time = response.getSingleInnerElementsNamed("time", NamespaceURIs.URN_XMPP_TIME);
        Assert.assertEquals(2, time.getInnerElements().size());
        
        XMLElement tzo = time.getSingleInnerElementsNamed("tzo", NamespaceURIs.URN_XMPP_TIME);
        
        Pattern tzPattern = Pattern.compile("[+-]\\d\\d:\\d\\d");
        Assert.assertTrue(tzPattern.matcher(tzo.getInnerText().getText()).matches());
        
        Pattern utcPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        XMLElement utc = time.getSingleInnerElementsNamed("utc", NamespaceURIs.URN_XMPP_TIME);
        Assert.assertTrue(utcPattern.matcher(utc.getInnerText().getText()).matches());
    }
}
