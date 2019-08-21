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
package org.apache.vysper.xmpp.server.s2s;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.S2SEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.cryptography.NonCheckingX509TrustManagerFactory;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunS2SServers extends TestCase {

    private static final String FILE_FILTER = "s2s-p";

    public static Test suite() {
        File testsDir = new File(".");
        File[] testFiles = testsDir.listFiles(new FilenameFilter() {
            public boolean accept(File arg0, String name) {
                return name.startsWith(FILE_FILTER);
            }
        });

        TestSuite suite = new TestSuite("S2S integration tests");
        for (File testFile : testFiles) {
            try {
                suite.addTest(new RunS2SServers(testFile));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        return suite;
    }

    private File testFile;

    private Properties config = new Properties();

    public RunS2SServers(File testFile) throws IOException {
        this.testFile = testFile;
        config.load(new FileInputStream(testFile));
    }

    @Override
    public String getName() {
        return testFile.getName();
    }

    protected void runTest() throws Throwable {
        Entity localServer = EntityImpl.parseUnchecked(config.getProperty("local.server"));
        String localConnect = config.getProperty("local.connect", localServer.getFullQualifiedName());
        Entity localUser = EntityImpl.parseUnchecked(config.getProperty("local.user"));
        String localPassword = "password";
        String remoteServer = config.getProperty("remote.server");
        String remoteUser = config.getProperty("remote.user");
        String remotePassword = config.getProperty("remote.password");

        String keystorePath = config.getProperty("keystore.path");
        String keystorePassword = config.getProperty("keystore.password");

        XMPPServer server = createLocalServer(localServer, localUser, localPassword, keystorePath, keystorePassword);

        Thread.sleep(2000);

        System.out.println();
        System.out.println();
        System.out.println("Connecting local client");
        System.out.println();
        System.out.println();

        LinkedBlockingQueue<Stanza> localClientPackages = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Stanza> remoteClientPackages = new LinkedBlockingQueue<>();

        XMPPTCPConnection localClient = connectClient(localConnect, localUser.getFullQualifiedName(), localPassword,
                keystorePath, keystorePassword, localClientPackages);
        // XMPPConnection localClient = null;

        System.out.println();
        System.out.println();
        System.out.println("Connecting remote client");
        System.out.println();
        System.out.println();

        // XMPPConnection remoteClient = connectClient(remoteServer, remoteUser,
        // remotePassword, keystorePath, keystorePassword, remoteClientPackages);
        XMPPTCPConnection remoteClient = null;

        Thread.sleep(3000);

        System.out.println();
        System.out.println();
        System.out.println("Sending message from local to remote");
        System.out.println();
        System.out.println();

        remoteClientPackages.clear();
        Message msg = new Message(JidCreate.from(remoteUser));
        msg.setBody("Hello world");

        localClient.sendStanza(msg);

        // Packet packet = remoteClientPackages.poll(15000, TimeUnit.MILLISECONDS);
        // if(packet != null && packet instanceof Message) {
        // System.out.println("!!!!!!" + ((Message)packet).getBody());
        // } else {
        // fail("Message not received by remote client");
        // }
        //
        // Thread.sleep(3000);
        //
        // System.out.println();
        // System.out.println();
        // System.out.println("Sending message from remote to local");
        // System.out.println();
        // System.out.println();
        //
        // localClientPackages.clear();
        // msg = new Message(localUser.getFullQualifiedName());
        // msg.setBody("Hello world");
        //
        // remoteClient.sendPacket(msg);
        //
        // packet = localClientPackages.poll(15000, TimeUnit.MILLISECONDS);
        // if(packet != null && packet instanceof Message) {
        // System.out.println("!!!!!!" + ((Message)packet).getBody());
        // } else {
        // fail("Message not received by local client");
        // }

        Thread.sleep(15000);
        System.out.println();
        System.out.println();
        System.out.println("Closing down");
        System.out.println();
        System.out.println();

        remoteClient.disconnect();
        localClient.disconnect();

        Thread.sleep(5000);

        server.stop();
    }

    private XMPPTCPConnection connectClient(String host, String user, String password, String keystorePath,
            String keystorePassword, final LinkedBlockingQueue<Stanza> packageQueue)
            throws XMPPException, IOException, InterruptedException, SmackException {
        XMPPTCPConnectionConfiguration connectionConfiguration = XMPPTCPConnectionConfiguration.builder().setHost(host)
                .setPort(5222).setXmppDomain("protocol7.com").setKeystorePath(keystorePath)
                .setCustomX509TrustManager(NonCheckingX509TrustManagerFactory.X509).build();

        XMPPTCPConnection client = new XMPPTCPConnection(connectionConfiguration);

        client.connect();
        client.login(user, password);
        client.addSyncStanzaListener(packet -> {
            System.out.println("# " + packet);
            packageQueue.add(packet);
        }, arg0 -> true);

        return client;
    }

    private XMPPServer createLocalServer(Entity localServer, Entity localUser, String password, String keystorePath,
            String keystorePassword) throws AccountCreationException, FileNotFoundException, Exception {
        XMPPServer server = new XMPPServer(localServer.getDomain());

        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        if (!accountManagement.verifyAccountExists(localUser)) {
            accountManagement.addUser(localUser, password);
        }

        // S2S endpoint
        server.addEndpoint(new S2SEndpoint());

        // C2S endpoint
        server.addEndpoint(new C2SEndpoint());

        server.setStorageProviderRegistry(providerRegistry);
        server.setTLSCertificateInfo(new File(keystorePath), keystorePassword);

        server.start();
        return server;
    }
}
