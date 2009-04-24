/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.vysper.xmpp.server;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.RecordingStanzaRelay;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Iterator;

/**
 * makes response available for testing
 */
public class TestSessionContext extends AbstractSessionContext implements StanzaWriter {

    private Queue<Stanza> recordedResponses = new LinkedList<Stanza>();
    private boolean closed = false;
    private boolean switchToTLSCalled;
    private boolean isReopeningXMLStream;
    private int recordedResponsesTotal = 0;

    public static TestSessionContext createSessionContext(Entity entity) {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        TestSessionContext sessionContext = new TestSessionContext(sessionStateHolder);
        if (entity != null) sessionContext.setInitiatingEntity(entity.getBareJID());
        return sessionContext;
    }

    public static TestSessionContext createWithStanzaReceiverRelayAuthenticated() {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        sessionStateHolder.setState(SessionState.AUTHENTICATED);
        return createWithStanzaReceiverRelay(sessionStateHolder);
    }
    
    public static TestSessionContext createWithStanzaReceiverRelay(SessionStateHolder sessionStateHolder) {
        StanzaReceiverRelay relay = new org.apache.vysper.xmpp.delivery.StanzaReceiverRelay();
        DefaultServerRuntimeContext serverContext = new DefaultServerRuntimeContext(new EntityImpl(null, "test", null), relay);
        relay.setServerRuntimeContext(serverContext);
        return new TestSessionContext(serverContext, sessionStateHolder);
    }

    public TestSessionContext(SessionStateHolder sessionStateHolder) {
        this(new DefaultServerRuntimeContext(new EntityImpl(null, "test", null), new RecordingStanzaRelay()), sessionStateHolder);
    }

    public TestSessionContext(ServerRuntimeContext serverRuntimeContext, SessionStateHolder sessionStateHolder) {
        super(serverRuntimeContext, sessionStateHolder);
        sessionId = serverRuntimeContext.getNextSessionId();
        xmlLang = "de";
    }

    public Stanza getNextRecordedResponse() {
        return recordedResponses.poll();
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
     * @param stanza records the stanza.
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

    public void switchToTLS() {
        switchToTLSCalled = true;
    }

    public void setIsReopeningXMLStream() {
        isReopeningXMLStream = true;
    }

    public boolean isSwitchToTLSCalled() {
        return switchToTLSCalled;
    }

    public StanzaReceiverQueue addReceiver(Entity entity, String resourceId) {
        if (!(getServerRuntimeContext().getStanzaRelay() instanceof StanzaReceiverRelay)) {
            throw new RuntimeException("cannot add receiver - the stanza relay is of a different kind");
        }
        StanzaReceiverQueue relay = new StanzaReceiverQueue();
        if (resourceId != null) entity = new EntityImpl(entity.getNode(), entity.getDomain(), resourceId);
        ((StanzaReceiverRelay) getServerRuntimeContext().getStanzaRelay()).add(entity, relay);
        return relay;
    }

    public int getRecordedResponsesTotal() {
        return recordedResponsesTotal;
    }
}
