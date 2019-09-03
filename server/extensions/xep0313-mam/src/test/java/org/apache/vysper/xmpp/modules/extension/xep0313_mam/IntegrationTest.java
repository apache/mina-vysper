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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.cryptography.NonCheckingX509TrustManagerFactory;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.in_memory.InMemoryMessageArchives;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Réda Housni Alaoui
 */
public abstract class IntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

    private static final String TLS_CERTIFICATE_PATH = "src/test/resources/bogus_mina_tls.cert";

    private static final String TLS_CERTIFICATE_PASSWORD = "boguspw";

    private static final String SERVER_DOMAIN = "vysper.org";

    private static final String PASSWORD = "password";

    private static final int DEFAULT_SERVER_PORT = 25222;

    private static final String ALICE_USERNAME = "test1@" + SERVER_DOMAIN;

    private static final String CAROL_USERNAME = "test2@" + SERVER_DOMAIN;

    private XMPPTCPConnection aliceClient;

    private XMPPTCPConnection carolClient;

    private XMPPServer server;

    private ToggleableOfflineStorageProvider offlineStorageProvider;

    @Before
    public void setUp() throws Exception {
        SmackConfiguration.setDefaultReplyTimeout(5000);
        offlineStorageProvider = new ToggleableOfflineStorageProvider();

        int port = findFreePort();

        startServer(port);

        aliceClient = connectClient(port, ALICE_USERNAME);
        carolClient = connectClient(port, CAROL_USERNAME);
    }

    protected AbstractXMPPConnection alice() {
        return aliceClient;
    }

    protected AbstractXMPPConnection carol() {
        return carolClient;
    }

    protected ToggleableOfflineStorageProvider offlineStorageProvider() {
        return offlineStorageProvider;
    }

    protected Stanza sendSync(XMPPConnection client, Stanza request)
            throws SmackException.NotConnectedException, InterruptedException {
        StanzaCollector collector = client.createStanzaCollector(new StanzaIdFilter(request.getStanzaId()));

        client.sendStanza(request);

        return collector.nextResult(5000);
    }

    private void startServer(int port) throws Exception {
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        AccountManagement accountManagement = providerRegistry.retrieve(AccountManagement.class);
        accountManagement.addUser(EntityImpl.parseUnchecked(ALICE_USERNAME), PASSWORD);
        accountManagement.addUser(EntityImpl.parseUnchecked(CAROL_USERNAME), PASSWORD);

        providerRegistry.add(offlineStorageProvider);

        server = new XMPPServer(SERVER_DOMAIN);

        TCPEndpoint endpoint = new C2SEndpoint();
        endpoint.setPort(port);
        server.addEndpoint(endpoint);
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File(TLS_CERTIFICATE_PATH), TLS_CERTIFICATE_PASSWORD);

        server.start();

        providerRegistry.add(new InMemoryMessageArchives());
        server.addModule(new MAMModule());

        Thread.sleep(200);
    }

    private XMPPTCPConnection connectClient(int port, String username) throws Exception {
        XMPPTCPConnectionConfiguration connectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                .setHost("localhost").setPort(port).setXmppDomain(SERVER_DOMAIN)
                .setHostnameVerifier((hostname, session) -> true).setCompressionEnabled(false)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .addEnabledSaslMechanism(SASLMechanism.PLAIN).setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE)
                .setKeystorePath(TLS_CERTIFICATE_PATH)
                .setCustomX509TrustManager(NonCheckingX509TrustManagerFactory.X509).build();

        XMPPTCPConnection client = new XMPPTCPConnection(connectionConfiguration);

        client.connect();

        client.login(username, PASSWORD);
        return client;
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
                }
            }
        }
    }

    @After
    public void tearDown() {
        try {
            aliceClient.disconnect();
        } catch (Exception ignored) {

        }

        try {
            carolClient.disconnect();
        } catch (Exception ignored) {

        }

        try {
            server.stop();
        } catch (Exception ignored) {
        }
    }

}
