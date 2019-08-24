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
package org.apache.vysper.xmpp.server.components;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.SimpleStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class ComponentStanzaProcessorTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private StanzaRelay stanzaRelay = Mockito.mock(StanzaRelay.class);

    private StanzaHandler handler = Mockito.mock(StanzaHandler.class);

    private Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, null, "body").build();

    private Stanza responseStanza = StanzaBuilder.createMessageStanza(TO, FROM, null, "response").build();

    private ResponseStanzaContainer container = new ResponseStanzaContainerImpl(responseStanza);

    private ComponentStanzaProcessor processor = new ComponentStanzaProcessor(stanzaRelay);

    @Before
    public void before() {
        Mockito.when(handler.verify(stanza)).thenReturn(true);
        Mockito.when(handler.getName()).thenReturn("message");
    }

    @Test(expected = RuntimeException.class)
    public void processNullStanza() {
        processor.processStanza(serverRuntimeContext, sessionContext, null, sessionStateHolder);
    }

    @Test(expected = RuntimeException.class)
    public void processNoneCoreStanza() {
        Stanza dummyStanza = new StanzaBuilder("foo", "bar").build();
        processor.processStanza(serverRuntimeContext, sessionContext, dummyStanza, sessionStateHolder);
    }

    @Test(expected = RuntimeException.class)
    public void processNoStanzaHandler() {
        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);
    }

    @Test
    public void processSuccessfulNoResponse() {
        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);

        Mockito.verifyZeroInteractions(serverRuntimeContext);
    }

    @Test
    public void processSuccessfulWithResponse() throws ProtocolException, DeliveryException {
        Mockito.when(handler.execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                new SimpleStanzaBroker(stanzaRelay, sessionContext))).thenReturn(container);

        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);

        Mockito.verify(stanzaRelay).relay(FROM, responseStanza, IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY);
    }

    @Test
    public void handlerThrowsException() throws ProtocolException, DeliveryException {
        Mockito.when(handler.execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                new SimpleStanzaBroker(stanzaRelay, sessionContext))).thenThrow(new ProtocolException());

        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);

        Mockito.verifyZeroInteractions(serverRuntimeContext);
    }

    @Test(expected = RuntimeException.class)
    public void processThenFailRelaying() throws ProtocolException, DeliveryException {
        Mockito.when(handler.execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                new SimpleStanzaBroker(stanzaRelay, sessionContext))).thenReturn(container);
        Mockito.doThrow(new DeliveryException()).when(stanzaRelay).relay(FROM, responseStanza,
                IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY);

        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);
    }

    @Test(expected = RuntimeException.class)
    public void processTLSEstablished() {
        processor.processTLSEstablished(sessionContext, sessionStateHolder);
    }

}
