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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XmppPinger {

    private static final Logger LOG = LoggerFactory.getLogger(XmppPinger.class);

    private String id = "xmppping-" + UUID.randomUUID().toString();
    private XmppPingIQHandler handler;
    
    private LinkedBlockingQueue<String> pingQueue = new LinkedBlockingQueue<String>(1);
    
    public XmppPinger(XmppPingIQHandler handler) {
        this.handler = handler;
    }

    public void ping(StanzaWriter stanzaWriter, Entity from, Entity to, int timeoutMillis, XmppPingListener listener) {
        handler.addPinger(this);
        
        Stanza ping = new StanzaBuilder("iq", NamespaceURIs.JABBER_SERVER)
            .addAttribute("from", from.getFullQualifiedName())
            .addAttribute("to", to.getFullQualifiedName())
            .addAttribute("type", "get")
            .addAttribute("id", id)
            .startInnerElement("ping", NamespaceURIs.URN_XMPP_PING).endInnerElement().build();
        
        stanzaWriter.write(ping);
        
        try {
            if(pingQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS) != null) {
                LOG.debug("pong received from " + to + " for ping id = " + id);
                listener.pong();
            } else {
                LOG.debug("no pong received for " + timeoutMillis + "msec from " + to + " for ping id = " + id);
                listener.timeout();
            }
        } catch (InterruptedException e) {
            // ignore
        } finally {
            handler.removePinger(this);
        }
        
    }

    protected void pong(String id) {
        if(this.id.equals(id)) {
            pingQueue.add(id);
        }
    }
}
