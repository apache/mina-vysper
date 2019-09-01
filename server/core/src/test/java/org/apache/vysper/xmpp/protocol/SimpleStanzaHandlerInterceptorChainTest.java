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
package org.apache.vysper.xmpp.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleStanzaHandlerInterceptorChainTest {

    private StanzaHandler stanzaHandler;

    private Stanza stanza;

    private ServerRuntimeContext serverRuntimeContext;

    private SessionContext sessionContext;

    private SessionStateHolder sessionStateHolder;

    private StanzaBroker firstStanzaBroker;

    @Before
    public void before() {
        stanzaHandler = mock(StanzaHandler.class);

        stanza = mock(Stanza.class);
        serverRuntimeContext = mock(ServerRuntimeContext.class);
        sessionContext = mock(SessionContext.class);
        sessionStateHolder = mock(SessionStateHolder.class);
        firstStanzaBroker = mock(StanzaBroker.class);
    }

    @Test
    public void test_no_interceptors() throws ProtocolException {
        new SimpleStanzaHandlerInterceptorChain(stanzaHandler, Collections.emptyList()).intercept(stanza,
                serverRuntimeContext, false, sessionContext, sessionStateHolder, firstStanzaBroker);

        verify(stanzaHandler).execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                firstStanzaBroker);
    }

    @Test
    public void test_two_interceptors() throws ProtocolException {
        List<StanzaHandlerInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new InterceptorMock());
        interceptors.add(new InterceptorMock());

        new SimpleStanzaHandlerInterceptorChain(stanzaHandler, interceptors).intercept(stanza, serverRuntimeContext,
                false, sessionContext, sessionStateHolder, firstStanzaBroker);

        verify(stanzaHandler).execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                firstStanzaBroker);

        assertTrue(((InterceptorMock) interceptors.get(0)).intercepted);
        assertTrue(((InterceptorMock) interceptors.get(1)).intercepted);
    }

    @Test
    public void test_stanzabroker_substitution_chaining() throws ProtocolException {
        StanzaBroker secondStanzaBroker = mock(StanzaBroker.class);
        StanzaBroker thirdStanzaBroker = mock(StanzaBroker.class);

        InterceptorMock firstInterceptor = new InterceptorMock().replaceStanzaBroker(secondStanzaBroker);
        InterceptorMock secondInterceptor = new InterceptorMock().replaceStanzaBroker(thirdStanzaBroker);

        List<StanzaHandlerInterceptor> interceptors = new ArrayList<>();
        interceptors.add(firstInterceptor);
        interceptors.add(secondInterceptor);

        new SimpleStanzaHandlerInterceptorChain(stanzaHandler, interceptors).intercept(stanza, serverRuntimeContext,
                false, sessionContext, sessionStateHolder, firstStanzaBroker);

        assertEquals(firstStanzaBroker, firstInterceptor.receivedStanzaBroker);
        assertEquals(secondStanzaBroker, secondInterceptor.receivedStanzaBroker);

        verify(stanzaHandler).execute(stanza, serverRuntimeContext, false, sessionContext, sessionStateHolder,
                thirdStanzaBroker);

    }

    private static class InterceptorMock implements StanzaHandlerInterceptor {

        private boolean intercepted;

        private StanzaBroker receivedStanzaBroker;

        private StanzaBroker replacingStanzaBroker;

        public InterceptorMock replaceStanzaBroker(StanzaBroker replacingStanzaBroker) {
            this.replacingStanzaBroker = replacingStanzaBroker;
            return this;
        }

        @Override
        public void intercept(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
                SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker,
                StanzaHandlerInterceptorChain interceptorChain) throws ProtocolException {

            receivedStanzaBroker = stanzaBroker;

            intercepted = true;

            if (replacingStanzaBroker != null) {
                stanzaBroker = replacingStanzaBroker;
            }

            interceptorChain.intercept(stanza, serverRuntimeContext, isOutboundStanza, sessionContext,
                    sessionStateHolder, stanzaBroker);
        }
    }

}