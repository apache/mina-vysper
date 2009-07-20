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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.worker.InboundStanzaProtocolWorker;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * relays stanzas to internal sessions, acts as a 'stage' by using a ThreadPoolExecutor
 * TODO: re-work the whole relay result
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DeliveringInboundStanzaRelay implements StanzaRelay {

    final Logger logger = LoggerFactory.getLogger(DeliveringInboundStanzaRelay.class);

    private static final InboundStanzaProtocolWorker INBOUND_STANZA_PROTOCOL_WORKER = new InboundStanzaProtocolWorker();

    protected ResourceRegistry resourceRegistry;
    protected ExecutorService executor;
    protected AccountManagement accountVerification;

    public DeliveringInboundStanzaRelay(ResourceRegistry resourceRegistry, StorageProviderRegistry storageProviderRegistry) {
        this(resourceRegistry, (AccountManagement)storageProviderRegistry.retrieve(AccountManagement.class));
    }
    
    public DeliveringInboundStanzaRelay(ResourceRegistry resourceRegistry, AccountManagement accountVerification) {
        this.resourceRegistry = resourceRegistry;
        this.accountVerification = accountVerification;
        int coreThreadCount = 10;
        int maxThreadCount = 20;
        int threadTimeoutSeconds = 2 * 60 * 1000;
        this.executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, threadTimeoutSeconds, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException {
        Future<RelayResult> resultFuture = executor.submit(new Relay(receiver, stanza, deliveryFailureStrategy));
    }

    private class Relay implements Callable<RelayResult> {
        private Entity receiver;
        private Stanza stanza;
        private DeliveryFailureStrategy deliveryFailureStrategy;
        protected final UnmodifyableSessionStateHolder sessionStateHolder = new UnmodifyableSessionStateHolder();

        Relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) {
            this.receiver = receiver;
            this.stanza = stanza;
            this.deliveryFailureStrategy = deliveryFailureStrategy;
        }

        public Entity getReceiver() {
            return receiver;
        }

        public Stanza getStanza() {
            return stanza;
        }

        public DeliveryFailureStrategy getDeliveryFailureStrategy() {
            return deliveryFailureStrategy;
        }

        public RelayResult call() {
            RelayResult relayResult = deliver();
            if (relayResult == null || relayResult.isRelayed()) return relayResult;
            return runFailureStrategy(relayResult);
        }

        private RelayResult runFailureStrategy(RelayResult relayResult) {
            if (deliveryFailureStrategy != null) {
                try {
                    deliveryFailureStrategy.process(stanza, relayResult.getProcessingError());
                } catch (DeliveryException e) {
                    return new RelayResult(e);
                } catch (RuntimeException e) {
                    return new RelayResult(new DeliveryException(e));
                }
            }
            // TODO throw relayResult.getProcessingError() in some appropriate context
            return relayResult;
        }

        protected RelayResult deliver() {
            List<RelayResult> relayResults = new ArrayList<RelayResult>();
            try {
                if (!accountVerification.verifyAccountExists(receiver)) {
                    logger.warn("cannot relay to unexisting receiver {} stanza {}", receiver.getFullQualifiedName(), stanza.toString());
                    return new RelayResult(new NoSuchLocalUserException());
                }
                List<SessionContext> receivingSessions = resourceRegistry.getSessions(receiver);
                if (receivingSessions == null || receivingSessions.size() == 0) {
                    logger.warn("cannot relay to offline receiver {} stanza {}", receiver.getFullQualifiedName(), stanza.toString());
                    return new RelayResult(new LocalRecipientOfflineException());
                } else {
                    relayToSessions(relayResults, receivingSessions);
                }
            } catch (RuntimeException e) {
                return new RelayResult(new DeliveryException(e));
            }

            // TODO handle this properly, don't only return the first failure
            for (RelayResult relayResult : relayResults) {
                if (!relayResult.isRelayed()) return relayResult;
            }
            return new RelayResult(); // return success result
        }

        protected void relayToSessions(List<RelayResult> relayResults, List<SessionContext> receivingSessions) {
            if (receivingSessions.size() > 1) {
                logger.warn("multiplexing: {} sessions will be processing {} ", receivingSessions.size(), stanza);
            }
            for (SessionContext sessionContext : receivingSessions) {
                if (sessionContext.getState() != SessionState.AUTHENTICATED) {
                    relayResults.add(new RelayResult(new DeliveryException("no relay to non-authenticated sessions")));
                    continue;
                }
                try {
                    StanzaHandler stanzaHandler = sessionContext.getServerRuntimeContext().getHandler(stanza);
                    INBOUND_STANZA_PROTOCOL_WORKER.processStanza(sessionContext, sessionStateHolder, stanza, stanzaHandler);
                } catch (Exception e) {
                    // TODO do not break out here. we should try to deliver to the others first!
                    relayResults.add(new RelayResult(new DeliveryException(e)));
                }
            }
        }
    }

    private static class UnmodifyableSessionStateHolder extends SessionStateHolder {

        @Override
        public void setState(SessionState newState) {
            throw new RuntimeException("unable to alter state");
        }

        @Override
        public SessionState getState() {
            return SessionState.AUTHENTICATED;
        }
    }

    private static class RelayResult {
        private DeliveryException processingError;
        private boolean relayed;

        public RelayResult(DeliveryException processingError) {
            this.processingError = processingError;
            this.relayed = false;
        }

        public RelayResult() {
            this.relayed = true;
        }

        public DeliveryException getProcessingError() {
            return processingError;
        }

        public boolean isRelayed() {
            return relayed;
        }
    }

}
