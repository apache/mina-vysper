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
package org.apache.vysper.xmpp.delivery.inbound;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.InternalServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnector;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

/**
 */
public class DeliveringExternalInboundStanzaRelayTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("to@vysper.org");

    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");

    private static final String LANG = "en";

    private static final String BODY = "Hello world";

    private static final Stanza STANZA = XMPPCoreStanza.getWrapper(StanzaBuilder.createMessageStanza(FROM, TO, LANG,
            BODY).build());
    
    private InternalSessionContext sessionContext = mock(InternalSessionContext.class);

    public void testRemoteServerError() throws Exception {
        XMPPServerConnectorRegistry registry = mock(XMPPServerConnectorRegistry.class);
        Mockito.when(registry.connect(SERVER)).thenThrow(new RemoteServerNotFoundException());

        InternalServerRuntimeContext serverRuntimeContext = mock(InternalServerRuntimeContext.class);
        Mockito.when(serverRuntimeContext.getServerConnectorRegistry()).thenReturn(registry);

        DeliveringExternalInboundStanzaRelay relay = new DeliveringExternalInboundStanzaRelay(new TestExecutorService());
        relay.setServerRuntimeContext(serverRuntimeContext);

        RecordingDeliveryFailureStrategy deliveryFailureStrategy = new RecordingDeliveryFailureStrategy();
        relay.relay(sessionContext, TO, STANZA, deliveryFailureStrategy);

        Stanza failedStanza = deliveryFailureStrategy.getRecordedStanza();
        Assert.assertNotNull(failedStanza);
        
        Assert.assertEquals("message", failedStanza.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_SERVER, failedStanza.getNamespaceURI());
        Assert.assertEquals(FROM, failedStanza.getFrom());
        Assert.assertEquals(TO, failedStanza.getTo());
    }

    public void testSuccessfulRelay() throws Exception {
        XMPPServerConnector connector = mock(XMPPServerConnector.class);
        
        XMPPServerConnectorRegistry registry = mock(XMPPServerConnectorRegistry.class);
        Mockito.when(registry.connect(SERVER)).thenReturn(connector);

        InternalServerRuntimeContext serverRuntimeContext = mock(InternalServerRuntimeContext.class);
        Mockito.when(serverRuntimeContext.getServerConnectorRegistry()).thenReturn(registry);

        DeliveringExternalInboundStanzaRelay relay = new DeliveringExternalInboundStanzaRelay(new TestExecutorService());
        relay.setServerRuntimeContext(serverRuntimeContext);

        RecordingDeliveryFailureStrategy deliveryFailureStrategy = new RecordingDeliveryFailureStrategy();
        relay.relay(sessionContext, TO, STANZA, deliveryFailureStrategy);

        Assert.assertNull(deliveryFailureStrategy.getRecordedStanza());

        ArgumentCaptor<Stanza> writtenStanzaCaptor = ArgumentCaptor.forClass(Stanza.class);
        Mockito.verify(connector).write(writtenStanzaCaptor.capture());
        
        Stanza writtenStanza = writtenStanzaCaptor.getValue();
        
        Assert.assertNotNull(writtenStanza);
        Assert.assertEquals("message", writtenStanza.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_SERVER, writtenStanza.getNamespaceURI());
        Assert.assertEquals(FROM, writtenStanza.getFrom());
        Assert.assertEquals(TO, writtenStanza.getTo());
    }

    public void testShutdown() throws DeliveryException {
        final ExecutorService testExecutorService = Executors.newFixedThreadPool(1);
        DeliveringExternalInboundStanzaRelay relay = new DeliveringExternalInboundStanzaRelay(testExecutorService);

        Assert.assertTrue(relay.isRelaying());
        relay.stop();
        Assert.assertFalse(relay.isRelaying());
        Assert.assertTrue(testExecutorService.isShutdown());

        try {
            relay.relay(sessionContext, TO, STANZA, null);
            Assert.fail("ServiceNotAvailableException expected");
        } catch (ServiceNotAvailableException e) {
            // test succeeds
        }
    }
}
