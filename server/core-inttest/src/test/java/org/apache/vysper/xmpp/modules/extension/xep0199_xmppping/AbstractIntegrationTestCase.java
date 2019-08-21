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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import junit.framework.TestCase;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.cryptography.NonCheckingX509TrustManagerFactory;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 */
public abstract class AbstractIntegrationTestCase extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTestCase.class);

    protected static final String SERVER_DOMAIN = "vysper.org";

    protected static final String TEST_USERNAME1 = "test1@vysper.org";

    protected static final String TEST_PASSWORD1 = "password";

    protected static final String TEST_USERNAME2 = "test2@vysper.org";

    protected static final String TEST_PASSWORD2 = "password";

    private static final int DEFAULT_SERVER_PORT = 25222;

    protected XMPPTCPConnection client;

    private XMPPServer server;

    protected int port;

    protected void addModules(XMPPServer server) {
        // default, do nothing
    }

    @Override
    protected void setUp() throws Exception {
        // make sure Smack times out after 5 seconds
        SmackConfiguration.setDefaultReplyTimeout(5000);

        port = findFreePort();

        startServer(port);

        client = connectClient(port, TEST_USERNAME1, TEST_PASSWORD1);
    }

    private void startServer(int port) throws Exception {
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        AccountManagement accountManagement = (AccountManagement) providerRegistry.retrieve(AccountManagement.class);
        accountManagement.addUser(EntityImpl.parseUnchecked(TEST_USERNAME1), TEST_PASSWORD1);
        accountManagement.addUser(EntityImpl.parseUnchecked(TEST_USERNAME2), TEST_PASSWORD2);

        server = new XMPPServer(SERVER_DOMAIN);

        TCPEndpoint endpoint = new C2SEndpoint();
        endpoint.setPort(port);
        server.addEndpoint(endpoint);
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File("src/main/config/bogus_mina_tls.cert"), "boguspw");

        server.start();

        addModules(server);

        // allow for the server to bootstrap
        Thread.sleep(200);
    }

    protected XMPPTCPConnection connectClient(int port, String username, String password) throws Exception {
        XMPPTCPConnectionConfiguration connectionConfiguration = XMPPTCPConnectionConfiguration
                .builder()
                .setHost("localhost")
                .setXmppDomain(SERVER_DOMAIN)
                .setPort(port)
                .setCompressionEnabled(false)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .addEnabledSaslMechanism(SASLMechanism.PLAIN)
                .setHostnameVerifier((hostname, session) -> true)
                .setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE)
                .setKeystorePath("src/main/config/bogus_mina_tls.cert")
                .setCustomX509TrustManager(NonCheckingX509TrustManagerFactory.X509)
                .build();

        XMPPTCPConnection client = new XMPPTCPConnection(connectionConfiguration);

        client.connect();
        client.login(username, password);

        return client;
    }

    protected Stanza sendSync(XMPPConnection client, Stanza request) throws SmackException.NotConnectedException, InterruptedException {
        // Create a packet collector to listen for a response.
        StanzaCollector collector = client.createStanzaCollector(new StanzaIdFilter(request.getStanzaId()));

        client.sendStanza(request);

        // Wait up to 5 seconds for a result.
        return collector.nextResult(5000);
    }

    private int findFreePort() {
        ServerSocket ss = null;

        // try using a predefined default port
        // makes netstat -a debugging easier
        try {
            ss = new ServerSocket(DEFAULT_SERVER_PORT);
            ss.setReuseAddress(true);

            // succeeded, return the default port
            logger.info("Test is using the default test port {}", DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        } catch (IOException e) {
            try {
                ss = new ServerSocket(0);
                ss.setReuseAddress(true);
                int port = ss.getLocalPort();
                logger.info("Failed to use default test port ({}), using {} instead", DEFAULT_SERVER_PORT, port);
                return port;
            } catch (IOException ee) {
                // we could not even open a random port so
                // the test will probably fail, anyways
                // return the default port
                return DEFAULT_SERVER_PORT;
            }
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException ignored) {
                    ;
                }
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            client.disconnect();
        } catch (Exception ignored) {
            ;
        }

        try {
            System.out.println("Test teardown, stopping server");
            server.stop();
        } catch (Exception ignored) {
            ;
        }
    }

}
