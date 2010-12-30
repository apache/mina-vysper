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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveredToOfflineReceiverException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.LocalRecipientOfflineException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnector;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * relays all 'incoming' stanzas to internal sessions, acts as a 'stage' by using a ThreadPoolExecutor
 * 'incoming' here means:
 * a. stanzas coming in from other servers
 * b. stanzas coming from other (local) sessions and are targeted to clients on this server
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DeliveringExternalInboundStanzaRelay implements StanzaRelay {

    final Logger logger = LoggerFactory.getLogger(DeliveringExternalInboundStanzaRelay.class);

    protected ExecutorService executor;

    protected OfflineStanzaReceiver offlineStanzaReceiver = null;

    protected ServerRuntimeContext serverRuntimeContext = null;

    public DeliveringExternalInboundStanzaRelay() {
        int coreThreadCount = 10;
        int maxThreadCount = 20;
        int threadTimeoutSeconds = 2 * 60 * 1000;
        this.executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, threadTimeoutSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        
        // rewrite the namespace into the jabber:server namespace
        stanza = StanzaBuilder.rewriteNamespace(stanza, NamespaceURIs.JABBER_CLIENT, NamespaceURIs.JABBER_SERVER);

        Future<RelayResult> resultFuture = executor.submit(new OutboundRelayCallable(receiver, stanza, deliveryFailureStrategy));
    }
    
    private class OutboundRelayCallable implements Callable<RelayResult> {
        private Entity receiver;

        private Stanza stanza;

        private DeliveryFailureStrategy deliveryFailureStrategy;

        OutboundRelayCallable(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) {
            this.receiver = receiver;
            this.stanza = stanza;
            this.deliveryFailureStrategy = deliveryFailureStrategy;
        }

        public RelayResult call() {
            RelayResult relayResult = deliver();
            if (relayResult == null || !relayResult.hasProcessingErrors())
                return relayResult;
            return runFailureStrategy(relayResult);
        }

        private RelayResult runFailureStrategy(RelayResult relayResult) {
            if (deliveryFailureStrategy != null) {
                try {
                    deliveryFailureStrategy.process(stanza, relayResult.getProcessingErrors());
                } catch (DeliveryException e) {
                    return new RelayResult(e);
                } catch (RuntimeException e) {
                    return new RelayResult(new DeliveryException(e));
                }
            }
            // TODO throw relayResult.getProcessingError() in some appropriate context
            return relayResult;
        }

        /**
         * @return
         */
        //@SpecCompliant(spec = "draft-ietf-xmpp-3921bis-00", section = "8.", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
        protected RelayResult deliver() {
            RelayResult relayResult = new RelayResult();
            try {
                XMPPServerConnector connector = serverRuntimeContext.getServerConnectorRegistry().getConnector(EntityImpl.parseUnchecked(stanza.getTo().getDomain()));
                
                connector.write(stanza);
            } catch (IOException e) {
                return new RelayResult(new DeliveryException(e));
            } catch (RuntimeException e) {
                return new RelayResult(new DeliveryException(e));
            }
            
            return relayResult;
        }
        
        private RelayResult relayNotPossible() {
            if (offlineStanzaReceiver != null) {
                offlineStanzaReceiver.receive(stanza);
                return new RelayResult(new DeliveredToOfflineReceiverException());
            } else {
                logger.warn("cannot relay to offline receiver {} stanza {}", receiver.getFullQualifiedName(), stanza
                        .toString());
                return new RelayResult(new LocalRecipientOfflineException());
            }
        }
    }
}
