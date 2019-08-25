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

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * Servlet for initiating websocket connections in Apache Tomcat. Requires
 * Tomcat 7.0.27 or later.
 * <p>
 * When creating this servlet from web.xml, the Vysper server needs to be
 * started beforehand (e.g. from a {@link ServletContextListener} and the
 * {@link ServerRuntimeContext} needs to be added as an attribute in the
 * {@link ServletContext} with the key
 * "org.apache.vysper.xmpp.server.ServerRuntimeContext".
 * </p>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TomcatXmppWebSocketServlet extends WebSocketServlet {

    /**
     * The attribute key for the {@link ServerRuntimeContext} in
     * {@link ServletContext}
     */
    public static final String SERVER_RUNTIME_CONTEXT_ATTRIBUTE = "org.apache.vysper.xmpp.server.ServerRuntimeContext";

    private static final long serialVersionUID = 197413099255392884L;

    private static final String SUB_PROTOCOL = "xmpp";

    private ServerRuntimeContext serverRuntimeContext;

    private StanzaProcessor stanzaProcessor;

    public TomcatXmppWebSocketServlet() {
        // default cstr needed
    }

    public TomcatXmppWebSocketServlet(ServerRuntimeContext serverRuntimeContext, StanzaProcessor stanzaProcessor) {
        this.serverRuntimeContext = serverRuntimeContext;
        this.stanzaProcessor = stanzaProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ServletException {
        super.init();

        if (serverRuntimeContext == null) {
            serverRuntimeContext = (ServerRuntimeContext) getServletContext()
                    .getAttribute(SERVER_RUNTIME_CONTEXT_ATTRIBUTE);
            if (serverRuntimeContext == null) {
                throw new RuntimeException("Failed to get Vysper ServerRuntimeContext from servlet context attribute \""
                        + SERVER_RUNTIME_CONTEXT_ATTRIBUTE + "\"");
            }
        }

        if (stanzaProcessor == null) {
            stanzaProcessor = (StanzaProcessor) getServletContext()
                    .getAttribute(StanzaProcessor.class.getCanonicalName());
            if (stanzaProcessor == null) {
                throw new RuntimeException("Failed to get Vysper StanzaProcessor from servlet context attribute \""
                        + StanzaProcessor.class.getCanonicalName() + "\"");
            }
        }
    }

    @Override
    protected String selectSubProtocol(List<String> subProtocols) {
        return SUB_PROTOCOL;
    }

    /**
     * {@inheritDoc}
     *
     * Will return null if the client does not provide the correct websocket sub
     * protocol. "xmpp" is required.
     */
    @Override
    protected StreamInbound createWebSocketInbound(String subProtocol) {
        // TODO subProtocol is always null on Tomcat 7.0.27, reactivate check when fixed
        // if (SUB_PROTOCOL.equals(subProtocol)) {
        TomcatXmppWebSocket sessionContext = new TomcatXmppWebSocket(serverRuntimeContext, stanzaProcessor);
        return sessionContext;
        // } else {
        // LOG.warn("Unsupported websocket sub protocol, must be \"xmpp\", but was \"" +
        // subProtocol + "\"");
        // return null;
        // }
    }
}
