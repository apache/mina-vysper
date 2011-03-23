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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Integration test for SOCKS5 mediated connections
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Runner {

    
    private static final String CHARSET = "ASCII";
    private static final String HELLO_WORLD = "hello world";

    private XMPPServer server;
    
    private XMPPConnection requestor;    
    private XMPPConnection target;
    
    @Before
    public void before() throws Exception {
        server = startServer();
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        requestor = connectClient("user1@vysper.org");
        
        target = connectClient("user2@vysper.org");
    }
  
    
    /*
     * This test requires that "vysper.org" and "socks.vysper.org" resolves to 127.0.0.1
     * and is therefore disabled by default.
     *  
     * On Linux/OS X, add the following to /etc/hosts:
     * 127.0.0.1   vysper.org
     * 127.0.0.1   socks.vysper.org
     */
    @Test
    @Ignore("Requires host resolution configuration, see comment")
    public void testTransfer() throws Exception {
        final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        
        Socks5BytestreamManager mng2 = Socks5BytestreamManager.getBytestreamManager(target);
        mng2.addIncomingBytestreamListener(new BytestreamListener() {
            public void incomingBytestreamRequest(BytestreamRequest request) {
                BytestreamSession session;
                try {
                    session = request.accept();
                    
                    byte[] b = new byte[HELLO_WORLD.getBytes(CHARSET).length];
                    InputStream in = session.getInputStream();
                    in.read(b);
                    in.close();
                    
                    queue.put(new String(b));
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            }
        });
        
        Thread.sleep(2000);
        System.out.println("##################");
        System.out.println("Starting SOCKS5 transfer");
        System.out.println("##################");
        
        String targetJid = requestor.getRoster().getPresence("user2@vysper.org").getFrom();
        
        Socks5BytestreamManager mng1 = Socks5BytestreamManager.getBytestreamManager(requestor);
        Socks5BytestreamSession session = mng1.establishSession(targetJid);
        OutputStream out = session.getOutputStream();
        out.write(HELLO_WORLD.getBytes(CHARSET));
        out.flush();
        out.close();
        
        Assert.assertEquals(HELLO_WORLD, queue.poll(10000, TimeUnit.MILLISECONDS));
    }

    
    @After
    public void after() {
        requestor.disconnect();
        target.disconnect();
        
        server.stop();
    }
    
    private XMPPConnection connectClient(String username) throws XMPPException {
        ConnectionConfiguration config1 = new ConnectionConfiguration("vysper.org", 5222);
        XMPPConnection conn1 = new XMPPConnection(config1);
        conn1.connect();
        conn1.login(username, "password");
        return conn1;
    }

    private XMPPServer startServer() throws Exception {
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        Entity user1 = EntityImpl.parse("user1@vysper.org");
        accountManagement.addUser(user1, "password");
        Entity user2 = EntityImpl.parse("user2@vysper.org");
        accountManagement.addUser(user2, "password");
        
        XMPPServer server = new XMPPServer("vysper.org");
        server.addEndpoint(new TCPEndpoint());
        server.setStorageProviderRegistry(providerRegistry);
        server.setTLSCertificateInfo(new File("src/test/resources/bogus_mina_tls.cert"), "boguspw");
        
        server.start();
        System.out.println("vysper server is running...");

        RosterManager rosterManager = (RosterManager) server.getServerRuntimeContext().getStorageProvider(RosterManager.class);
        rosterManager.addContact(user1, new RosterItem(user2, SubscriptionType.BOTH));
        rosterManager.addContact(user2, new RosterItem(user1, SubscriptionType.BOTH));
        
        server.addModule(new Socks5Module("socks"));
        
        return server;
    }
    
}
