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
package org.apache.vysper.console;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * Extends {@link XMPPConnection} to add support for synchronous
 * request-response
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ExtendedXMPPConnection extends XMPPTCPConnection {

    public ExtendedXMPPConnection(XMPPTCPConnectionConfiguration config) {
        super(config);
    }

//    public ExtendedXMPPConnection(String serviceName) {
//        super(serviceName);
//    }

    public static class IdPacketFilter implements StanzaFilter {
        private String id;

        public IdPacketFilter(String id) {
            this.id = id;
        }

        @Override
        public boolean accept(Stanza stanza) {
            return id.equals(stanza.getStanzaId());
        }
    }

    public static class SyncPacketListener implements StanzaListener {
        private LinkedBlockingQueue<Stanza> queue;

        public SyncPacketListener(LinkedBlockingQueue<Stanza> queue) {
            this.queue = queue;
        }

        @Override
        public void processStanza(Stanza packet) {
            queue.offer(packet);
        }
    }

    /**
     * Send a request and wait for the response.
     * 
     * @param request
     * @return
     * @throws InterruptedException
     */
    public Stanza sendSync(Stanza request) throws InterruptedException, SmackException.NotConnectedException {
        LinkedBlockingQueue<Stanza> queue = new LinkedBlockingQueue<>();
        StanzaListener listener = new SyncPacketListener(queue);
        StanzaFilter filter = new IdPacketFilter(request.getPacketID());

        addSyncStanzaListener(listener, filter);
        sendStanza(request);

        Stanza response = queue.poll(10000, TimeUnit.MILLISECONDS);
        removeSyncStanzaListener(listener);

        return response;
    }
}
