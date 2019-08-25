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
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows HTTP clients to communicate via the WebSocket protocol with Vysper.
 * See http://tools.ietf.org/html/draft-moffitt-xmpp-over-websocket-00
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class WebSocketEndpoint implements Endpoint {

    protected final static Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);

    protected ServerRuntimeContext serverRuntimeContext;
    
    private StanzaProcessor stanzaProcessor;

    protected int port = 8080;

    protected Server server;

    protected boolean isSSLEnabled;

    protected String sslKeystorePath;

    protected String sslKeystorePassword;

    protected String contextPath = "/";

    /**
     * {@inheritDoc}
     */
    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    @Override
    public void setStanzaProcessor(StanzaProcessor stanzaProcessor) {
        this.stanzaProcessor = stanzaProcessor;
    }

    /**
     * Set the port on which the endpoint will listen for incoming traffic.
     * Defaults to 8080.
     * @param port The TCP/IP listener port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Configures the SSL keystore
     * <p>
     * Required if SSL is enabled. Also, setting the keystore password is
     * required.
     * @param keystorePath The path to the Java keystore
     * @param password The password for the Java keystore
     */
    public void setSSLCertificateKeystore(String keystorePath, String password) {
        sslKeystorePath = keystorePath;
        sslKeystorePassword = password;
    }

    /**
     * Enables/disables SSL for this endpoint.
     * <p>
     * If SSL is enabled it requires SSL certificate information that can be
     * configured with {@link #setSSLCertificateInfo(String, String)}
     * @param value
     */
    public void setSSLEnabled(boolean value) {
        isSSLEnabled = value;
    }

    /**
     * Determines the context URI where the websocket transport will be accessible.
     * The default is as 'root context' under '/'.
     * @param contextPath
     */
    public void setContextPath(String contextPath) {
        if (contextPath == null) contextPath = "/";
        this.contextPath = contextPath;
    }

    /**
     * create a basic Jetty server including a connector on the configured port
     * override in subclass to create a different kind of setup or to reuse an existing instance
     * @return
     */
    protected Server createJettyServer() {
        Server server = new Server();

        Connector connector;
        if (isSSLEnabled) {
            SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
            sslConnector.setKeystore(sslKeystorePath);
            sslConnector.setPassword(sslKeystorePassword);
            sslConnector.setKeyPassword(sslKeystorePassword);
            connector = sslConnector;
        } else {
            connector = new SelectChannelConnector();
        }
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });
        return server;
    }

    /**
     * create handler for XMPP over websockets.
     * for a different handler setup, override in a subclass.
     * for more than one handler, add them to a org.eclipse.jetty.server.handler.ContextHandlerCollection
     * and return the collection
     * @return
     */
    protected Handler createHandler() {
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath(contextPath);

        JettyXmppWebSocketServlet wsServlet = new JettyXmppWebSocketServlet(serverRuntimeContext, stanzaProcessor);
        servletContext.addServlet(new ServletHolder(wsServlet), "/ws");

        return servletContext;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RuntimeException a wrapper of the possible {@link java.lang.Exception} that Jetty can throw at start-up
     */
    public void start() throws IOException {
        server = createJettyServer();
        Handler wsHandler = createHandler();

        Handler existingHandler = server.getHandler();
        if(existingHandler != null && existingHandler instanceof HandlerCollection) {
            ((HandlerCollection)existingHandler).addHandler(wsHandler);
        } else {
            server.setHandler(wsHandler);
        }

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.warn("Could not stop the Jetty server", e);
        }
    }

}
