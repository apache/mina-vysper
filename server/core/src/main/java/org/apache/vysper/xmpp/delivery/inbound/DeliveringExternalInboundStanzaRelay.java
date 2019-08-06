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
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.InternalServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.resources.ManagedThreadPool;
import org.apache.vysper.xmpp.server.resources.ManagedThreadPoolUtil;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnector;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
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
public class DeliveringExternalInboundStanzaRelay implements StanzaRelay, ManagedThreadPool {

    final Logger logger = LoggerFactory.getLogger(DeliveringExternalInboundStanzaRelay.class);

    private static class RejectedDeliveryHandler implements RejectedExecutionHandler {
        
        DeliveringExternalInboundStanzaRelay relay;
        Logger logger;

        private RejectedDeliveryHandler(DeliveringExternalInboundStanzaRelay relay, Logger logger) {
            this.relay = relay;
            this.logger = logger;
        }

        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            logger.info("relaying of external inbound stanza has been rejected");
        }
    }
    
    
    protected ExecutorService executor;

    protected OfflineStanzaReceiver offlineStanzaReceiver = null;

    protected InternalServerRuntimeContext serverRuntimeContext = null;
    
    protected long lastCompleted = 0;
    protected long lastDumpTimestamp = 0;

    public DeliveringExternalInboundStanzaRelay() {
        int coreThreadCount = 10;
        int maxThreadCount = 20;
        int threadTimeoutSeconds = 2 * 60;
        this.executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, threadTimeoutSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new RejectedDeliveryHandler(this, logger));
    }

    /*package*/ DeliveringExternalInboundStanzaRelay(ExecutorService executor) {
        this.executor = executor;
    }

    public void setServerRuntimeContext(InternalServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void setMaxThreadCount(int maxThreadPoolCount) {
        if (!(executor instanceof ThreadPoolExecutor)) {
            throw new IllegalStateException("cannot set max thread count for " + executor.getClass());
        }
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)executor;
        threadPoolExecutor.setCorePoolSize(maxThreadPoolCount);
        threadPoolExecutor.setMaximumPoolSize(2*maxThreadPoolCount);
    }
    
    public void setThreadTimeoutSeconds(int threadTimeoutSeconds) {
        if (!(executor instanceof ThreadPoolExecutor)) {
            throw new IllegalStateException("cannot set thread timeout for " + executor.getClass());
        }
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)executor;
        threadPoolExecutor.setKeepAliveTime(threadTimeoutSeconds, TimeUnit.SECONDS);
    }
    
    public void dumpThreadPoolInfo(Writer writer) throws IOException {
        if (!(executor instanceof ThreadPoolExecutor)) {
            throw new IllegalStateException("cannot dump info for " + executor.getClass());
        }
        ThreadPoolExecutor pool = (ThreadPoolExecutor)executor;

        final long now = System.currentTimeMillis();
        writer.append("==== externalRelay:").append("\n");
        ManagedThreadPoolUtil.writeThreadPoolInfo(writer, pool);
        final long completedTaskCount = pool.getCompletedTaskCount();
        if (lastDumpTimestamp > 0) {
            writer.append("throughput=\t").append(Long.toString(completedTaskCount - lastCompleted))
                                          .append(" per ").append(Long.toString(now - lastDumpTimestamp)).append("\n");
        }
        lastDumpTimestamp = now;
        lastCompleted = completedTaskCount;
    }

    public void relay(InternalSessionContext sessionContext, Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        
        if (!isRelaying()) {
            throw new ServiceNotAvailableException("external inbound relay is not relaying");
        }
        
        // rewrite the namespace into the jabber:server namespace
        stanza = StanzaBuilder.rewriteNamespace(stanza, NamespaceURIs.JABBER_CLIENT, NamespaceURIs.JABBER_SERVER);
        XMPPCoreStanza coreStanza = XMPPCoreStanza.getWrapper(stanza);
        
        if(coreStanza != null) {
            Future<RelayResult> resultFuture = executor.submit(new OutboundRelayCallable(coreStanza, deliveryFailureStrategy));
        } else {
            // ignore non-core stanzas
        }
    }

    public boolean isRelaying() {
        return !executor.isShutdown();
    }

    public void stop() {
        this.executor.shutdown();
    }
    
    private class OutboundRelayCallable implements Callable<RelayResult> {
        private XMPPCoreStanza stanza;

        private DeliveryFailureStrategy deliveryFailureStrategy;

        OutboundRelayCallable(XMPPCoreStanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) {
            this.stanza = stanza;
            this.deliveryFailureStrategy = deliveryFailureStrategy;
        }

        public RelayResult call() {
            RelayResult relayResult = deliver();

            if (relayResult == null || !relayResult.hasProcessingErrors()) {
                return relayResult;
            } else {
                return runFailureStrategy(relayResult);
            }
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
        @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
        protected RelayResult deliver() {
            try {
                RelayResult relayResult = new RelayResult();
                XMPPServerConnector connector = serverRuntimeContext.getServerConnectorRegistry().connect(EntityImpl.parseUnchecked(stanza.getTo().getDomain()));
                
                connector.write(stanza);
                return relayResult.setProcessed();
            } catch (DeliveryException e) {
                return new RelayResult(e);
            }
        }
    }
}
