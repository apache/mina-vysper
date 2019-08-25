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

import junit.framework.Assert;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class DefaultXMPPServerConnectorRegistryTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");
    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
    private SessionContext sessionContext = Mockito.mock(SessionContext.class);
    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    @Before
    public void before() {
        SSLContext sslContext = Mockito.mock(SSLContext.class);
        Mockito.when(serverRuntimeContext.getSslContext()).thenReturn(sslContext);
        Mockito.when(serverRuntimeContext.getServerEntity()).thenReturn(TO);
        
        Mockito.when(sessionContext.getInitiatingEntity()).thenReturn(FROM);
        Mockito.when(sessionContext.getSessionId()).thenReturn("session-id");

        sessionStateHolder.setState(SessionState.STARTED);
    }

    @Test
    public void connectorShouldBeReused() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnectorRegistry registry = 
                new DefaultXMPPServerConnectorRegistry(serverRuntimeContext, null, null) {
            @Override
            protected XMPPServerConnector createConnector(Entity otherServer,
                    ServerRuntimeContext serverRuntimeContext, SessionContext dialbackSessionContext,
                    SessionStateHolder dialbackSessionStateHolder) {
                return Mockito.mock(XMPPServerConnector.class);
            }
        };
        
        XMPPServerConnector actualConnector = registry.connect(TO);
        Assert.assertNotNull(actualConnector);
        
        XMPPServerConnector actualConnector2 = registry.connect(TO);
        
        // connectors should be reused
        Assert.assertSame(actualConnector, actualConnector2);
        
        Mockito.verify(actualConnector).start();
        Mockito.verify(actualConnector2).start();
    }
    
    @Test
    public void dontReuseClosedConnector() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnectorRegistry registry = 
                new DefaultXMPPServerConnectorRegistry(serverRuntimeContext, null, null) {
            @Override
            protected XMPPServerConnector createConnector(Entity otherServer,
                    ServerRuntimeContext serverRuntimeContext, SessionContext dialbackSessionContext,
                    SessionStateHolder dialbackSessionStateHolder) {
                XMPPServerConnector connector = Mockito.mock(XMPPServerConnector.class);
                Mockito.when(connector.isClosed()).thenReturn(true);
                return connector;
            }
        };
        
        XMPPServerConnector actualConnector = registry.connect(TO);
        Assert.assertNotNull(actualConnector);
        
        XMPPServerConnector actualConnector2 = registry.connect(TO);
        
        Assert.assertNotSame(actualConnector, actualConnector2);
    }

    @Test
    public void dontReuseConnectorToDifferentServers() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnectorRegistry registry = 
                new DefaultXMPPServerConnectorRegistry(serverRuntimeContext, null, null) {
            @Override
            protected XMPPServerConnector createConnector(Entity otherServer,
                    ServerRuntimeContext serverRuntimeContext, SessionContext dialbackSessionContext,
                    SessionStateHolder dialbackSessionStateHolder) {
                return Mockito.mock(XMPPServerConnector.class);
            }
        };
        
        XMPPServerConnector actualConnector = registry.connect(TO);
        Assert.assertNotNull(actualConnector);
        
        XMPPServerConnector actualConnector2 = registry.connect(EntityImpl.parseUnchecked("foo.org"));
        
        Assert.assertNotSame(actualConnector, actualConnector2);
    }

    @Test
    public void createDialbackConnector() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnectorRegistry registry = 
                new DefaultXMPPServerConnectorRegistry(serverRuntimeContext, null, null) {
            @Override
            protected XMPPServerConnector createConnector(Entity otherServer,
                    ServerRuntimeContext serverRuntimeContext, SessionContext dialbackSessionContext,
                    SessionStateHolder dialbackSessionStateHolder) {
                return Mockito.mock(XMPPServerConnector.class);
            }
        };
        
        SessionContext dialbackSessionContext = Mockito.mock(SessionContext.class);
        SessionStateHolder dialbackSessionStateHolder = new SessionStateHolder();

        XMPPServerConnector actualConnector = registry.connectForDialback(TO, dialbackSessionContext, dialbackSessionStateHolder);
        Assert.assertNotNull(actualConnector);
        
        Mockito.verify(actualConnector).start();
    }

    @Test
    public void close() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnectorRegistry registry = 
                new DefaultXMPPServerConnectorRegistry(serverRuntimeContext, null, null) {
            @Override
            protected XMPPServerConnector createConnector(Entity otherServer,
                    ServerRuntimeContext serverRuntimeContext, SessionContext dialbackSessionContext,
                    SessionStateHolder dialbackSessionStateHolder) {
                return Mockito.mock(XMPPServerConnector.class);
            }
        };
        
        XMPPServerConnector actualConnector = registry.connect(TO);
        XMPPServerConnector actualConnector2 = registry.connect(TO);

        registry.close();
        
        Mockito.verify(actualConnector).close();
        Mockito.verify(actualConnector2).close();
    }
    

}
