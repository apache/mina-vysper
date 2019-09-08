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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptorChain;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class MAMStanzaHandlerInterceptorTest {

    private static final Entity ALICE = EntityImpl.parseUnchecked("alice@foo.com");

    private static final Entity BOB = EntityImpl.parseUnchecked("bob@foo.com");

    private ServerRuntimeContext serverRuntimeContext;

    private SessionContext sessionContext;

    private SessionStateHolder sessionStateHolder;

    private StanzaBroker nonArchivingStanzaBroker;

    private StanzaBroker archivingStanzaBroker;

    private StanzaHandlerInterceptorChain interceptorChain;

    private MAMStanzaBrokerProviderMock mamStanzaBrokerProvider;

    private MAMStanzaHandlerInterceptor tested;

    @Before
    public void before() {
        serverRuntimeContext = mock(ServerRuntimeContext.class);
        sessionContext = mock(SessionContext.class);
        sessionStateHolder = mock(SessionStateHolder.class);
        nonArchivingStanzaBroker = mock(StanzaBroker.class);
        archivingStanzaBroker = mock(StanzaBroker.class);
        interceptorChain = mock(StanzaHandlerInterceptorChain.class);

        mamStanzaBrokerProvider = new MAMStanzaBrokerProviderMock();

        tested = new MAMStanzaHandlerInterceptor(Collections.singletonList(mamStanzaBrokerProvider));
    }

    @Test
    public void stanzaWithHintNoStoreShouldNotBeStored() throws ProtocolException {
        Stanza stanza = StanzaBuilder.createMessageStanza(ALICE, BOB, "en", null)
                .startInnerElement("no-store", NamespaceURIs.XEP0334_MESSAGE_PROCESSING_HINTS).endInnerElement()
                .build();

        tested.intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker, interceptorChain);

        verify(interceptorChain).intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker);
    }

    @Test
    public void stanzaWithHintNoPermanentStoreShouldNotBeStored() throws ProtocolException {
        Stanza stanza = StanzaBuilder.createMessageStanza(ALICE, BOB, "en", null)
                .startInnerElement("no-permanent-store", NamespaceURIs.XEP0334_MESSAGE_PROCESSING_HINTS)
                .endInnerElement().build();

        tested.intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker, interceptorChain);

        verify(interceptorChain).intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker);
    }

    @Test
    public void stanzaWithHintStoreShouldBeStored() throws ProtocolException {
        Stanza stanza = StanzaBuilder.createMessageStanza(ALICE, BOB, "en", null)
                .startInnerElement("store", NamespaceURIs.XEP0334_MESSAGE_PROCESSING_HINTS).endInnerElement().build();

        tested.intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker, interceptorChain);

        verify(interceptorChain).intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                archivingStanzaBroker);
        assertTrue(mamStanzaBrokerProvider.archivingForced);
    }

    @Test
    public void stanzaWithoutHintShouldBeStored() throws ProtocolException {
        Stanza stanza = StanzaBuilder.createMessageStanza(ALICE, BOB, "en", null).build();

        tested.intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                nonArchivingStanzaBroker, interceptorChain);

        verify(interceptorChain).intercept(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                archivingStanzaBroker);
        assertFalse(mamStanzaBrokerProvider.archivingForced);
    }

    private class MAMStanzaBrokerProviderMock implements MAMStanzaBrokerProvider {

        private boolean archivingForced;

        @Override
        public boolean supports(Stanza processedStanza, ServerRuntimeContext serverRuntimeContext) {
            return true;
        }

        @Override
        public StanzaBroker proxy(StanzaBroker delegate, ServerRuntimeContext serverRuntimeContext,
                SessionContext sessionContext, boolean isOutboundStanza, boolean forceArchiving) {
            this.archivingForced = forceArchiving;
            return archivingStanzaBroker;
        }
    }
}