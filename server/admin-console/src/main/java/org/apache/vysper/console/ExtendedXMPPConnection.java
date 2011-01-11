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

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

public class ExtendedXMPPConnection extends XMPPConnection {
    
    public ExtendedXMPPConnection(ConnectionConfiguration config, CallbackHandler callbackHandler) {
        super(config, callbackHandler);
    }

    public ExtendedXMPPConnection(ConnectionConfiguration config) {
        super(config);
    }

    public ExtendedXMPPConnection(String serviceName, CallbackHandler callbackHandler) {
        super(serviceName, callbackHandler);
    }

    public ExtendedXMPPConnection(String serviceName) {
        super(serviceName);
    }

    public static class IdPacketFilter implements PacketFilter {
        private String id;

        public IdPacketFilter(String id) {
            this.id = id;
        }

        public boolean accept(Packet packet) {
            return id.equals(packet.getPacketID());
        }
    }
    
    public static class SyncPacketListener implements PacketListener {
        private LinkedBlockingQueue<Packet> queue;

        public SyncPacketListener(LinkedBlockingQueue<Packet> queue) {
            this.queue = queue;
        }

        public void processPacket(Packet packet) {
            queue.offer(packet);
        }
    }
    
    public Packet sendSync(Packet request) throws InterruptedException {
        LinkedBlockingQueue<Packet> queue = new LinkedBlockingQueue<Packet>();
        PacketListener listener = new SyncPacketListener(queue);
        PacketFilter filter = new IdPacketFilter(request.getPacketID());
        
        addPacketListener(listener, filter);
        sendPacket(request);
        
        Packet response = queue.poll(10000, TimeUnit.MILLISECONDS);
        removePacketListener(listener);
        
        return response;
    }
}
