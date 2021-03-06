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
package org.apache.vysper.xmpp.extension.websockets;

import java.io.IOException;

import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized {@link SessionContext} for Jetty Websocket endpoints.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JettyXmppWebSocket implements WebSocket, WebSocket.OnTextMessage, Outbound {

    private final static Logger LOG = LoggerFactory.getLogger(JettyXmppWebSocket.class);

    private WebSocketBackedSessionContext sessionContext;

    private Connection outbound;

    public JettyXmppWebSocket(ServerRuntimeContext serverRuntimeContext, StanzaProcessor stanzaProcessor) {
        this.sessionContext = new WebSocketBackedSessionContext(serverRuntimeContext, stanzaProcessor, this);
    }

    /**
     * {@inheritDoc}
     */
    public void onOpen(Connection outbound) {
        LOG.info("WebSocket client connected");
        this.outbound = outbound;

        sessionContext.onOpen();
    }

    public void onMessage(String data) {
        LOG.info("< " + data);
        sessionContext.onMessage(data);
    }

    /**
     * {@inheritDoc}
     */
    public void onClose(int closeCode, String message) {
        LOG.info("WebSocket client disconnected with Code " + closeCode + " with " + message);

        sessionContext.onClose();
    }

    public void write(String xml) throws IOException {
        LOG.info("> " + xml);
        outbound.sendMessage(xml);
    }
}
