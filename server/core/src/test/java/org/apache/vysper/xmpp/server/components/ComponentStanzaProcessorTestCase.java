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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.protocol.SimpleStanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class ComponentStanzaProcessorTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("other.org");

    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);

    private InternalSessionContext sessionContext = mock(InternalSessionContext.class);

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private StanzaRelay stanzaRelay = mock(StanzaRelay.class);

    private StanzaHandler handler = mock(StanzaHandler.class);

    private Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, null, "body").build();

    private ComponentStanzaProcessor processor = new ComponentStanzaProcessor(
            new SimpleStanzaHandlerExecutorFactory(stanzaRelay));

    private StanzaWriter sessionContextStanzaWriter;

    @Before
    public void before() {
        when(handler.verify(stanza)).thenReturn(true);
        when(handler.getName()).thenReturn("message");

        sessionContextStanzaWriter = mock(StanzaWriter.class);
        when(sessionContext.getResponseWriter()).thenReturn(sessionContextStanzaWriter);
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
    public void processSuccessfulWithResponse() throws ProtocolException {
        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);

        verify(handler).execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                new DefaultStanzaBroker(stanzaRelay, sessionContext));
    }

    @Test(expected = RuntimeException.class)
    public void processThenFailRelaying() throws ProtocolException {
        // when(handler.execute(stanza, serverRuntimeContext, false, sessionContext,
        // sessionStateHolder,
        // new SimpleStanzaBroker(stanzaRelay, sessionContext))).thenReturn(container);

        doThrow(new RuntimeException()).when(handler).execute(stanza, serverRuntimeContext, false, sessionContext,
                sessionStateHolder, new DefaultStanzaBroker(stanzaRelay, sessionContext));

        processor.addHandler(handler);

        processor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);
    }

    @Test(expected = RuntimeException.class)
    public void processTLSEstablished() {
        processor.processTLSEstablished(sessionContext, sessionStateHolder);
    }

}
