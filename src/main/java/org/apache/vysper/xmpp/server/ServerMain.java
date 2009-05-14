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
package org.apache.vysper.xmpp.server;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;

import java.io.File;

/**
 * starts the server as a standalone application
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerMain {

    /**
     * boots the server as a standalone application
     * found on the classpath
     * @param args
     */
    public static void main(String[] args) throws AccountCreationException, EntityFormatException {

        // choose the storage you want to use
        //StorageProviderRegistry providerRegistry = new JcrStorageProviderRegistry();
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        
        final AccountManagement accountManagement = (AccountManagement) providerRegistry.retrieve(AccountManagement.class);

        if(!accountManagement.verifyAccountExists(EntityImpl.parse("user1@vysper.org"))) accountManagement.addUser("user1@vysper.org", "password1");
        if(!accountManagement.verifyAccountExists(EntityImpl.parse("user2@vysper.org"))) accountManagement.addUser("user2@vysper.org", "password1");
        if(!accountManagement.verifyAccountExists(EntityImpl.parse("user3@vysper.org"))) accountManagement.addUser("user3@vysper.org", "password1");

        XMPPServer server = new XMPPServer("vysper.org");
        server.addEndpoint(new TCPEndpoint());
        //server.addEndpoint(new StanzaSessionFactory());
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File("src/main/config/bogus_mina_tls.cert"), "boguspw");

        try {
            server.start();
            System.out.println("vysper server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        server.addModule(new VcardTempModule());
        server.addModule(new PublishSubscribeModule());
    }
}
