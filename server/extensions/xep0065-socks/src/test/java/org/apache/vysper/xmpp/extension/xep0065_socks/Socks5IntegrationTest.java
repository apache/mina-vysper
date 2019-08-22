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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Integration test for SOCKS5 mediated connections
 *
 * This test requires that "vysper.org" and "socks.vysper.org" resolves to
 * 127.0.0.1 and is therefore disabled by default.
 * 
 * On Linux/OS X, add the following to /etc/hosts: 127.0.0.1 vysper.org
 * 127.0.0.1 socks.vysper.org
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@Ignore("Requires host resolution configuration, see comment")
public class Socks5IntegrationTest {

    private static final String SUBDOMAIN = "socks";

    private static final String SERVER = "vysper.org";

    private static final Entity USER1 = EntityImpl.parseUnchecked("user1@vysper.org");

    private static final Entity USER2 = EntityImpl.parseUnchecked("user2@vysper.org");

    private static final String PASSWORD = "password";

    private static final String CHARSET = "ASCII";

    private static final String TEST_DATA = "hello world";

    private XMPPServer server;

    private XMPPTCPConnection requestor;

    private XMPPTCPConnection target;

    @Before
    public void before() throws Exception {
        server = startServer();

        requestor = connectClient(USER1);
        target = connectClient(USER2);
    }

    @Test
    public void medidiatedConnectionTransfer() throws Exception {
        // add support for mediated connections
        server.addModule(new Socks5Module(SUBDOMAIN));

        // disable direct connections
        Socks5Proxy.setLocalSocks5ProxyEnabled(false);

        assertTransfer();
    }

    @Test
    public void directConnectionTransfer() throws Exception {
        // enable direct connections
        Socks5Proxy.setLocalSocks5ProxyEnabled(true);

        assertTransfer();
    }

    private void assertTransfer()
            throws InterruptedException, XMPPException, IOException, UnsupportedEncodingException, SmackException {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

        Socks5BytestreamManager mng2 = Socks5BytestreamManager.getBytestreamManager(target);
        mng2.addIncomingBytestreamListener(new TestByteStreamListener(queue));

        // allow for clients to initiate
        Thread.sleep(2000);
        System.out.println("##################");
        System.out.println("Starting SOCKS5 transfer");
        System.out.println("##################");

        Jid targetJid = Roster.getInstanceFor(requestor).getPresence(JidCreate.bareFrom(USER2.getFullQualifiedName()))
                .getFrom();

        Socks5BytestreamManager mng1 = Socks5BytestreamManager.getBytestreamManager(requestor);
        Socks5BytestreamSession session = mng1.establishSession(targetJid);
        OutputStream out = session.getOutputStream();
        out.write(TEST_DATA.getBytes(CHARSET));
        out.flush();
        out.close();

        Assert.assertEquals(TEST_DATA, queue.poll(10000, TimeUnit.MILLISECONDS));
    }

    private final class TestByteStreamListener implements BytestreamListener {
        private final LinkedBlockingQueue<String> queue;

        private TestByteStreamListener(LinkedBlockingQueue<String> queue) {
            this.queue = queue;
        }

        public void incomingBytestreamRequest(BytestreamRequest request) {
            BytestreamSession session;
            try {
                session = request.accept();

                byte[] b = new byte[TEST_DATA.getBytes(CHARSET).length];
                InputStream in = session.getInputStream();
                in.read(b);
                in.close();

                queue.put(new String(b));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @After
    public void after() {
        requestor.disconnect();
        target.disconnect();

        server.stop();
    }

    private XMPPTCPConnection connectClient(Entity username)
            throws XMPPException, InterruptedException, IOException, SmackException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder().setXmppDomain(SERVER)
                .setPort(5222).build();
        XMPPTCPConnection conn = new XMPPTCPConnection(config);
        conn.connect();
        conn.login(username.getFullQualifiedName(), PASSWORD);
        return conn;
    }

    private XMPPServer startServer() throws Exception {
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        accountManagement.addUser(USER1, PASSWORD);
        accountManagement.addUser(USER2, PASSWORD);

        XMPPServer server = new XMPPServer(SERVER);
        server.addEndpoint(new C2SEndpoint());
        server.setStorageProviderRegistry(providerRegistry);
        server.setTLSCertificateInfo(new File("src/test/resources/bogus_mina_tls.cert"), "boguspw");

        server.start();
        System.out.println("vysper server is running...");

        RosterManager rosterManager = server.getServerRuntimeContext()
                .getStorageProvider(RosterManager.class);
        rosterManager.addContact(USER1, new RosterItem(USER2, SubscriptionType.BOTH));
        rosterManager.addContact(USER2, new RosterItem(USER1, SubscriptionType.BOTH));

        return server;
    }

}
