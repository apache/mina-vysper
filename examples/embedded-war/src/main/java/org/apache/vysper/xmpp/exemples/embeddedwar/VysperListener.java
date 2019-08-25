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
package org.apache.vysper.xmpp.exemples.embeddedwar;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.S2SEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.XMPPServer;

public class VysperListener implements ServletContextListener {

    private XMPPServer server;

    public void contextInitialized(ServletContextEvent sce) {
        try {
            String domain = "vysper.org";
            
            StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
    
            final AccountManagement accountManagement = providerRegistry
                    .retrieve(AccountManagement.class);

            Entity user1 = EntityImpl.parse("user1@" + domain);
            if (!accountManagement.verifyAccountExists(user1)) {
                accountManagement.addUser(user1, "password1");
            }

            final String pathToTLSCertificate = "/WEB-INF/bogus_mina_tls.cert";

            server = new XMPPServer(domain);
            // enable classic TCP bases access
            server.addEndpoint(new C2SEndpoint());
            
            // enable bosh
            final BoshEndpoint boshEndpoint = new BoshEndpoint();
            boshEndpoint.setContextPath("/bosh");
            boshEndpoint.setPort(8090);
            server.addEndpoint(boshEndpoint);

            // allow XMPP federation
            server.addEndpoint(new S2SEndpoint());
            
            server.setStorageProviderRegistry(providerRegistry);

            server.setTLSCertificateInfo(sce.getServletContext().getResourceAsStream(pathToTLSCertificate), "boguspw");
    
            try {
                server.start();
                System.out.println("vysper server is running...");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            final ServerFeatures serverFeatures = server.getServerRuntimeContext().getServerFeatures();
            serverFeatures.setRelayingToFederationServers(true);
            serverFeatures.setCheckFederationServerCertificates(false);
            
            server.addModule(new SoftwareVersionModule());
            server.addModule(new EntityTimeModule());
            server.addModule(new VcardTempModule());
            server.addModule(new XmppPingModule());

            // Used by the websocket endpoint, if enabled
            sce.getServletContext().setAttribute("org.apache.vysper.xmpp.server.ServerRuntimeContext", server.getServerRuntimeContext());
            sce.getServletContext().setAttribute(StanzaProcessor.class.getCanonicalName(), server.getStanzaProcessor());
            sce.getServletContext().setAttribute("vysper", server);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        server.stop();
    }

}
