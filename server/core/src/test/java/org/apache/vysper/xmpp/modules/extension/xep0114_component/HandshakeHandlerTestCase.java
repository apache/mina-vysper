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

package org.apache.vysper.xmpp.modules.extension.xep0114_component;

import junit.framework.TestCase;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionContext.SessionTerminationCause;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.mockito.Mockito;

/**
 */
public class HandshakeHandlerTestCase extends TestCase {

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);
    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
    private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    private ComponentAuthentication componentAuthentication = Mockito.mock(ComponentAuthentication.class);

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final String SECRET = "sekrit";
    private static final String STREAM_ID = "123456";

    private static final String HANDSHAKE = DigestUtils.sha256Hex(STREAM_ID + SECRET).toLowerCase();
    
    private HandshakeHandler handler = new HandshakeHandler();
    
    @Override
    public void setUp() throws AccountCreationException {
        Mockito.when(serverRuntimeContext.getStorageProvider(ComponentAuthentication.class)).thenReturn(componentAuthentication);
        Mockito.when(componentAuthentication.verifyCredentials(FROM, HANDSHAKE, STREAM_ID)).thenReturn(true);
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
        
        Mockito.when(sessionContext.getSessionId()).thenReturn(STREAM_ID);
        
        sessionStateHolder.setState(SessionState.INITIATED);
    }
    
    public void testValid() throws AccountCreationException {
        Stanza request = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).addText(HANDSHAKE).build();
        
        ResponseStanzaContainer container = handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder);
        
        Assert.assertNotNull(container.getResponseStanza());
        Assert.assertEquals(SessionState.AUTHENTICATED, sessionStateHolder.getState());
    }

    public void testInvalid() throws AccountCreationException {
        Stanza request = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).addText("123").build();
        
        ResponseStanzaContainer container = handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder);
        
        Assert.assertNull(container);
        Mockito.verify(sessionContext).endSession(SessionTerminationCause.STREAM_ERROR);
        Assert.assertEquals(SessionState.INITIATED, sessionStateHolder.getState());
    }

    public void testMissingComponentAuthentication() throws AccountCreationException {
        Mockito.when(serverRuntimeContext.getStorageProvider(ComponentAuthentication.class)).thenReturn(null);
        
        Stanza request = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).addText(HANDSHAKE).build();
        
        ResponseStanzaContainer container = handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder);
        
        Assert.assertNull(container);
        Mockito.verify(sessionContext).endSession(SessionTerminationCause.STREAM_ERROR);
        Assert.assertEquals(SessionState.INITIATED, sessionStateHolder.getState());
    }

    public void testNoText() throws AccountCreationException {
        Stanza request = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).build();
        
        ResponseStanzaContainer container = handler.execute(request, serverRuntimeContext, true, sessionContext, sessionStateHolder);
        
        Assert.assertNull(container);
        Mockito.verify(sessionContext).endSession(SessionTerminationCause.STREAM_ERROR);
        Assert.assertEquals(SessionState.INITIATED, sessionStateHolder.getState());
    }

    public void testVerify() throws AccountCreationException {
        Stanza stanza = new StanzaBuilder("handshake", NamespaceURIs.JABBER_COMPONENT_ACCEPT).build();

        Assert.assertTrue(handler.verify(stanza));
    }

    public void testVerifyInvalidNamespace() throws AccountCreationException {
        Stanza stanza = new StanzaBuilder("handshake", NamespaceURIs.JABBER_SERVER).build();

        Assert.assertFalse(handler.verify(stanza));
    }

    public void testVerifyInvalidName() throws AccountCreationException {
        Stanza stanza = new StanzaBuilder("dummy", NamespaceURIs.JABBER_COMPONENT_ACCEPT).build();

        Assert.assertFalse(handler.verify(stanza));
    }

}
