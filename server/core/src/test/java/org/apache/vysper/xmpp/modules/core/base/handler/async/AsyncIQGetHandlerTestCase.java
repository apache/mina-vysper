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
package org.apache.vysper.xmpp.modules.core.base.handler.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

import junit.framework.TestCase;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AsyncIQGetHandlerTestCase extends TestCase {

    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private RecordingStanzaBroker stanzaBroker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = new TestSessionContext(sessionStateHolder);
        stanzaBroker = new RecordingStanzaBroker();
    }

    protected static final int SLEEP_INTERVAL = 50;

    private static class AsyncIQGetHandler extends AbstractAsyncIQGetHandler implements Executor {
        protected TriggeredRunnableFuture triggeredRunnableFuture;

        public AsyncIQGetHandler() {
            super();
            super.serviceExecutor = this;
        }

        @Override
        protected RunnableFuture<XMPPCoreStanza> createGetTask(IQStanza stanza,
                ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
            triggeredRunnableFuture = new TriggeredRunnableFuture(stanza, serverRuntimeContext, sessionContext);
            return triggeredRunnableFuture;
        }

        public TriggeredRunnableFuture getWaitingRunnableFuture() {
            return triggeredRunnableFuture;
        }

        public void execute(Runnable runnable) {
            new Thread(runnable).start();
        }
    }

    private static class TriggeredRunnableFuture extends ResponseFuture<XMPPCoreStanza> {

        protected boolean done = false;

        private boolean triggerDelivery = false; // to signal from test when to proceed

        protected XMPPCoreStanza response = null;

        protected TriggeredRunnableFuture(XMPPCoreStanza requestStanza, ServerRuntimeContext serverRuntimeContext,
                SessionContext sessionContext) {
            super(requestStanza, serverRuntimeContext, sessionContext);
        }

        public boolean cancel(boolean b) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return done;
        }

        public XMPPCoreStanza get() throws InterruptedException, ExecutionException {
            return response;
        }

        public XMPPCoreStanza get(long l, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return response;
        }

        public void run() {
            try {
                while (!triggerDelivery) {
                    try {
                        Thread.sleep(SLEEP_INTERVAL);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                try {
                    Stanza finalStanza = StanzaBuilder
                            .createIQStanza(requestStanza.getTo(), requestStanza.getFrom(), IQStanzaType.RESULT,
                                    requestStanza.getID())
                            .startInnerElement("success", NamespaceURIs.JABBER_CLIENT).endInnerElement().build();
                    response = XMPPCoreStanza.getWrapper(finalStanza);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } finally {
                done = true;
            }
        }

        public void triggerDelivery() {
            triggerDelivery = true;
        }
    }

    public void testAsyncGet() throws ExecutionException, InterruptedException {
        AsyncIQGetHandler asyncIQGetHandler = new AsyncIQGetHandler();

        assertNull("future is create on execute", asyncIQGetHandler.getWaitingRunnableFuture());

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(new EntityImpl("test", "vysper.org", null), null,
                IQStanzaType.GET, "id1");
        stanzaBuilder.startInnerElement("query", NamespaceURIs.JABBER_CLIENT).endInnerElement();
        Stanza iqStanza = stanzaBuilder.build();

        asyncIQGetHandler.execute(iqStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);
        assertFalse(stanzaBroker.hasStanzaWrittenToSession());

        TriggeredRunnableFuture runnableFuture = asyncIQGetHandler.getWaitingRunnableFuture();
        assertNotNull("future has been created", runnableFuture);
        assertFalse("not done", runnableFuture.isDone());

        runnableFuture.triggerDelivery();

        while (!runnableFuture.isDone()) {
            try {
                Thread.sleep(SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                ;
            }
        }

        XMPPCoreStanza coreStanza = runnableFuture.get();
        assertNotNull(coreStanza);

    }
}
