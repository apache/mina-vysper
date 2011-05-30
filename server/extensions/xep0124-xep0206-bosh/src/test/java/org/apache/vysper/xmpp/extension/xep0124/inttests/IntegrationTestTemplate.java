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
package org.apache.vysper.xmpp.extension.xep0124.inttests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;
import org.apache.vysper.xmpp.extension.xep0124.XMLUtil;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.junit.After;
import org.junit.Before;
import org.xml.sax.SAXException;


public class IntegrationTestTemplate {

    protected HttpClient httpclient = new DefaultHttpClient();
    protected XMPPServer server;
    
    protected int serverPort;
    
    private int findFreePort() throws IOException {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(0);
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        } finally {
            if(ss != null) {
                ss.close();
            }
        }
    }
    
    protected String getServerUrl() {
        return "http://localhost:" + serverPort + "/";
    }
    
    protected Stanza sendRequest(String request) throws ClientProtocolException, IOException, IllegalStateException, SAXException {
        HttpPost post = new HttpPost(getServerUrl());
        HttpEntity entity = new StringEntity(request);
        post.setEntity(entity);
        HttpResponse response = httpclient.execute(post);
        
        assertNotNull(response.getEntity());
        Stanza boshResponse = new XMLUtil(response.getEntity().getContent()).parse();

        assertEquals("body", boshResponse.getName());
        assertEquals(NamespaceURIs.XEP0124_BOSH, boshResponse.getNamespaceURI());
        return boshResponse;
    }

    protected BoshEndpoint processBoshEndpoint(BoshEndpoint endpoint) {
        // default, do nothing
        return endpoint;
    }
    
    @Before
    public void startServer() throws Exception {
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        Entity user1 = EntityImpl.parseUnchecked("user1@vysper.org");
        if (!accountManagement.verifyAccountExists(user1)) {
            accountManagement.addUser(user1, "password1");
        }
        
        server = new XMPPServer("vysper.org");
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File("src/test/resources/bogus_mina_tls.cert"), "boguspw");
        
        BoshEndpoint boshEndpoint = new BoshEndpoint();
        int port = findFreePort();
        boshEndpoint.setPort(port);
        
        boshEndpoint = processBoshEndpoint(boshEndpoint);
        
        server.addEndpoint(boshEndpoint);
        
        
        server.start();
        System.out.println("Vysper BOSH server running on port " + port);

        serverPort = port;
    }
    
    @After
    public void tearDown() throws Exception {
        server.stop();
        httpclient.getConnectionManager().shutdown();
    }

}
