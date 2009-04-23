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

import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;

import java.util.concurrent.*;

/**
 * stanza processor, acts as a 'stage' by using a ThreadPoolExecutor
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class QueuedStanzaProcessor implements StanzaProcessor {

    private final ResponseWriter responseWriter = new ResponseWriter();

    protected ExecutorService executor;

    protected StanzaProcessor protocolWorker = new ProtocolWorker();

    public QueuedStanzaProcessor() {
        int coreThreadCount = 10;
        int maxThreadCount = 20;
        int threadTimeoutSeconds = 2 * 60 * 1000;
        executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, threadTimeoutSeconds, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza, SessionStateHolder sessionStateHolder) {
        executor.submit(new StanzaProcessorUnitOfWork(sessionContext, stanza, sessionStateHolder));
    }

    public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        ProtocolWorker.processTLSEstablishedInternal(sessionContext, sessionStateHolder, responseWriter);
    }

    private class StanzaProcessorUnitOfWork implements Runnable {

        private SessionContext sessionContext;
        private Stanza stanza;
        private SessionStateHolder sessionStateHolder;

        private StanzaProcessorUnitOfWork(SessionContext sessionContext, Stanza stanza, SessionStateHolder sessionStateHolder) {
            this.sessionContext = sessionContext;
            this.stanza = stanza;
            this.sessionStateHolder = sessionStateHolder;
        }

        public void run() {
            protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza, sessionStateHolder);
        }
    }

}
