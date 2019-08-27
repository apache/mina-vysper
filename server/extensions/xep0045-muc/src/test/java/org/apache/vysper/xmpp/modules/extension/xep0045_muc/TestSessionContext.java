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

package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.RecordingStanzaRelay;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.protocol.ProtocolWorker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.SimpleStanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;

/**
 * makes response available for testing
 */
public class TestSessionContext extends AbstractSessionContext implements StanzaWriter {

    private LinkedBlockingQueue<Stanza> recordedResponses = new LinkedBlockingQueue<Stanza>();

    private boolean closed = false;

    private boolean switchToTLSCalled;

    private boolean isReopeningXMLStream;

    private int recordedResponsesTotal = 0;

    private final StanzaRelay relay;

    /**
     * creates a new session context (but doesn't set the runtime context)
     *
     * @param entity
     * @return
     */
    public static TestSessionContext createSessionContext(Entity entity) {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        TestSessionContext sessionContext = new TestSessionContext(sessionStateHolder);
        if (entity != null)
            sessionContext.setInitiatingEntity(entity.getBareJID());
        return sessionContext;
    }

    /**
     * creates a new authenticated session and a new runtime context
     *
     * @return
     */
    public static TestSessionContext createWithStanzaReceiverRelayAuthenticated() {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        sessionStateHolder.setState(SessionState.AUTHENTICATED);
        return createWithStanzaReceiverRelay(sessionStateHolder);
    }

    /**
     * creates a session and also creates a fresh runtime context for it
     *
     * @param sessionStateHolder
     * @return
     */
    public static TestSessionContext createWithStanzaReceiverRelay(SessionStateHolder sessionStateHolder) {
        StanzaReceiverRelay relay = new org.apache.vysper.xmpp.delivery.StanzaReceiverRelay();
        DefaultServerRuntimeContext serverContext = new DefaultServerRuntimeContext(new EntityImpl(null, "test", null),
                relay);
        relay.setServerRuntimeContext(serverContext);
        return new TestSessionContext(serverContext, sessionStateHolder, relay);
    }

    /**
     * creates another session for an already created runtime context
     *
     * @param sessionStateHolder
     * @param serverContext
     * @return
     */
    public static TestSessionContext createWithStanzaReceiverRelay(SessionStateHolder sessionStateHolder,
            ServerRuntimeContext serverContext, StanzaReceiverRelay relay) {
        relay.setServerRuntimeContext(serverContext);
        return new TestSessionContext(serverContext, sessionStateHolder, relay);
    }

    public StanzaRelay getStanzaRelay() {
        return relay;
    }

    public TestSessionContext(SessionStateHolder sessionStateHolder) {
        this(sessionStateHolder, new RecordingStanzaRelay());
    }

    public TestSessionContext(ServerRuntimeContext serverRuntimeContext, SessionStateHolder sessionStateHolder,
            StanzaRelay relay) {
        super(serverRuntimeContext, new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(relay)),
                sessionStateHolder);
        sessionId = serverRuntimeContext.getNextSessionId();
        xmlLang = "de";
        this.relay = relay;
    }

    public TestSessionContext(SessionStateHolder sessionStateHolder, StanzaRelay relay) {
        super(new DefaultServerRuntimeContext(new EntityImpl(null, "test", null), relay,
                new MemoryStorageProviderRegistry()),
                new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(relay)), sessionStateHolder);
        sessionId = serverRuntimeContext.getNextSessionId();
        xmlLang = "de";
        this.relay = relay;
    }

    public Stanza getNextRecordedResponse() {
        return recordedResponses.poll();
    }

    public Stanza getNextRecordedResponse(long maxWaitMillis) {
        try {
            return recordedResponses.poll(maxWaitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public Stanza getNextRecordedResponseForResource(String resource) {
        for (Iterator<Stanza> it = recordedResponses.iterator(); it.hasNext();) {
            Stanza recordedResponse = it.next();
            if (recordedResponse.getTo() != null && recordedResponse.getTo().isResourceSet()) {
                if (recordedResponse.getTo().getResource().equals(resource)) {
                    it.remove();
                    return recordedResponse;
                }
            }
        }
        return null;
    }

    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed; // checks if close had been called
    }

    /**
     * Resets all recorded stanzas and their count.
     */
    public void reset() {
        recordedResponses.clear();
        recordedResponsesTotal = 0;
    }

    /**
     * @param stanza
     *            records the stanza.
     */
    public void write(Stanza stanza) {
        recordedResponses.add(stanza);
        recordedResponsesTotal++;
    }

    public void setSessionState(SessionState sessionState) {
        this.sessionStateHolder.setState(sessionState);
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void switchToTLS(boolean delayed, boolean clientTls) {
        switchToTLSCalled = true;
    }

    public void setIsReopeningXMLStream() {
        isReopeningXMLStream = true;
    }

    public boolean isSwitchToTLSCalled() {
        return switchToTLSCalled;
    }

    public StanzaReceiverQueue addReceiver(Entity entity, String resourceId) {
        if (!(this.relay instanceof StanzaReceiverRelay)) {
            throw new RuntimeException("cannot add receiver - the stanza relay is of a different kind");
        }
        StanzaReceiverQueue relay = new StanzaReceiverQueue();
        if (resourceId != null)
            entity = new EntityImpl(entity.getNode(), entity.getDomain(), resourceId);
        ((StanzaReceiverRelay) this.relay).add(entity, relay);
        return relay;
    }

    public int getRecordedResponsesTotal() {
        return recordedResponsesTotal;
    }
}
