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
package org.apache.vysper.xmpp.extension.xep0124;

import java.io.IOException;

import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows HTTP clients to communicate via the BOSH protocol with Vysper.
 * <p>
 * See http://xmpp.org/extensions/xep-0124.html and
 * http://xmpp.org/extensions/xep-0206.html
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshEndpoint implements Endpoint {

    private final Logger logger = LoggerFactory.getLogger(BoshEndpoint.class);

    private ServerRuntimeContext serverRuntimeContext;

    private int port = 8080;

    private Server server;

    private boolean isSSLEnabled;

    private String sslKeystorePath;

    private String sslKeystorePassword;

    private String flashCrossDomainPolicy;

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * Setter for the listen port
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Configures the SSL keystore and the keystore password.
     * <p>
     * These parameters are required if SSL is enabled.
     * The password is used both for accessing the keystore and for recovering
     * the key from the keystore. The unique password is a limitation, you
     * cannot use different passwords for the keystore and for the key.
     * 
     * @param keystorePath the path to the Java keystore
     * @param password the password used as the keystore password and also used
     * when recovering the key from the keystore
     */
    public void setSSLCertificateInfo(String keystorePath, String password) {
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
     * Setter for the Flash cross-domain policy file location
     * @param policyPath
     */
    public void setFlashCrossDomainPolicy(String policyPath) {
        flashCrossDomainPolicy = policyPath;
    }

    /**
     * @throws IOException 
     * @throws RuntimeException a wrapper of the possible
     * {@link java.lang.Exception} that Jetty can throw at start-up
     */
    public void start() throws IOException {
        server = new Server();

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

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        BoshServlet boshServlet = new BoshServlet();
        boshServlet.setServerRuntimeContext(serverRuntimeContext);
        
        if(flashCrossDomainPolicy != null) {
            boshServlet.setFlashCrossDomainPolicy(flashCrossDomainPolicy);
        }
        context.addServlet(new ServletHolder(boshServlet), "/");

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.warn("Could not stop the Jetty server", e);
        }
    }

}
