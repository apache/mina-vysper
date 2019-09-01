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
package org.apache.vysper.xmpp.delivery;

import junit.framework.Assert;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class StanzaRelayBrokerTestCase extends Mockito {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity INTERNAL = EntityImpl.parseUnchecked("to@vysper.org");
    private static final Entity EXTERNAL = EntityImpl.parseUnchecked("to@other.org");
    private static final Entity COMPONENT = EntityImpl.parseUnchecked("foo.vysper.org");
    private static final Entity COMPONENT_USER = EntityImpl.parseUnchecked("user@foo.vysper.org");
    
    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");
    private static final String LANG = "en";
    private static final String BODY = "Hello world";
    
    private DeliveryFailureStrategy failureStrategy = IgnoreFailureStrategy.INSTANCE;
    
    private StanzaRelay internalRelay = mock(StanzaRelay.class);
    private StanzaRelay externalRelay = mock(StanzaRelay.class);
    private InternalSessionContext sessionContext = mock(InternalSessionContext.class);
    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);
    private ServerFeatures serverFeatures = mock(ServerFeatures.class);

    private StanzaRelayBroker broker = new StanzaRelayBroker();
    
    private Stanza stanza = StanzaBuilder.createMessageStanza(FROM, INTERNAL, LANG, BODY).build();
    
    @Before
    public void before() {
        when(serverRuntimeContext.getServerEntity()).thenReturn(SERVER);
        when(serverRuntimeContext.getServerFeatures()).thenReturn(serverFeatures);
        
        broker.setExternalRelay(externalRelay);
        broker.setInternalRelay(internalRelay);
        broker.setServerRuntimeContext(serverRuntimeContext);

    }
    
    @Test
    public void toInternal() throws DeliveryException {
        broker.relay(sessionContext, INTERNAL, stanza, failureStrategy);

        verify(internalRelay).relay(sessionContext, INTERNAL, stanza, failureStrategy);
        verifyZeroInteractions(externalRelay);
    }

    @Test
    public void toExternalWithFederation() throws DeliveryException {
        when(serverFeatures.isRelayingToFederationServers()).thenReturn(true);
        
        broker.relay(sessionContext, EXTERNAL, stanza, failureStrategy);

        verifyZeroInteractions(internalRelay);
        verify(externalRelay).relay(sessionContext, EXTERNAL, stanza, failureStrategy);
    }
    
    @Test
    public void toComponent() throws DeliveryException {
        when(serverFeatures.isRelayingToFederationServers()).thenReturn(true);
        
        broker.relay(sessionContext, COMPONENT, stanza, failureStrategy);
        
        verify(internalRelay).relay(sessionContext, COMPONENT, stanza, failureStrategy);
        verifyZeroInteractions(externalRelay);
    }
    
    @Test
    public void toComponentUser() throws DeliveryException {
        when(serverFeatures.isRelayingToFederationServers()).thenReturn(true);
        
        broker.relay(sessionContext, COMPONENT_USER, stanza, failureStrategy);
        
        verify(internalRelay).relay(sessionContext, COMPONENT_USER, stanza, failureStrategy);
        verifyZeroInteractions(externalRelay);
    }
    
    @Test(expected=IllegalStateException.class)
    public void toExternalWithoutFederation() throws DeliveryException {
        when(serverFeatures.isRelayingToFederationServers()).thenReturn(false);
        
        broker.relay(sessionContext, EXTERNAL, stanza, failureStrategy);
    }
    
    @Test(expected=RuntimeException.class)
    public void toServer() throws DeliveryException {
        broker.relay(sessionContext, SERVER, stanza, failureStrategy);
    }
    
    @Test(expected=RuntimeException.class)
    public void toNullReceiver() throws DeliveryException {
        broker.relay(sessionContext, null, stanza, failureStrategy);
    }

    @Test
    public void shutdown() {
        Assert.assertTrue(broker.isRelaying());
        broker.stop();
        Assert.assertFalse(broker.isRelaying());
        
        try {
            broker.relay(sessionContext, INTERNAL, stanza, null);
            Assert.fail("ServiceNotAvailableException expected");
        } catch (ServiceNotAvailableException e) {
            // test succeeds
        } catch (DeliveryException e) {
            Assert.fail("unexpected delivery exception");
        }
    }   
}
