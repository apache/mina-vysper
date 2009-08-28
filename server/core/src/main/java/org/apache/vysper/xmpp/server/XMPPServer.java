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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.cryptography.BogusTrustManagerFactory;
import org.apache.vysper.xmpp.cryptography.FileBasedTLSContextFactory;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringInboundStanzaRelay;
import org.apache.vysper.xmpp.delivery.RecordingStanzaRelay;
import org.apache.vysper.xmpp.delivery.StanzaRelayBroker;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.roster.RosterModule;
import org.apache.vysper.xmpp.modules.servicediscovery.ServiceDiscoveryModule;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 * this class is able to boot a standalone XMPP server.
 * <code>
 * XMPPServer server = new XMPPServer("vysper.org");
 *
 * server.setUserAuthorization(...); // add user authorization class
 * server.addEndpoint(...); // add endpoints, at least one
 * server.setTLSCertificateInfo(...); //
 *
 * server.start(); // inits all endpoints and default internals
 * </code>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPServer {

    private final List<SASLMechanism> saslMechanisms = new ArrayList<SASLMechanism>();
    private String serverDomain;
    private DefaultServerRuntimeContext serverRuntimeContext;
    private StorageProviderRegistry storageProviderRegistry;
    private File tlsCertificateFile;
    private String tlsCertificatePassword;
    private final List<Endpoint> endpoints = new ArrayList<Endpoint>();

    public XMPPServer(String domain) {
        this.serverDomain = domain;

        // default list of SASL mechanisms
        saslMechanisms.add(new Plain());
    }

    public void setSASLMechanisms(List<SASLMechanism> validMechanisms) {
        saslMechanisms.addAll(validMechanisms);
    }

    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry) {
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public void setTLSCertificateInfo(File certificate, String password) {
        tlsCertificateFile = certificate;
        tlsCertificatePassword = password;
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void start() throws Exception {

        BogusTrustManagerFactory bogusTrustManagerFactory = new BogusTrustManagerFactory();
        FileBasedTLSContextFactory tlsContextFactory = new FileBasedTLSContextFactory(tlsCertificateFile);
        tlsContextFactory.setPassword(tlsCertificatePassword);
        tlsContextFactory.setTrustManagerFactory(bogusTrustManagerFactory);

        List<NamespaceHandlerDictionary> dictionaries = new ArrayList<NamespaceHandlerDictionary>();
        addCoreDictionaries(dictionaries);

        ResourceRegistry resourceRegistry = new ResourceRegistry();

        EntityImpl serverEntity = new EntityImpl(null, serverDomain, null);

        AccountManagement accountManagement = (AccountManagement) storageProviderRegistry.retrieve(AccountManagement.class);
        DeliveringInboundStanzaRelay internalStanzaRelay = new DeliveringInboundStanzaRelay(serverEntity, resourceRegistry, accountManagement);
        RecordingStanzaRelay externalStanzaRelay = new RecordingStanzaRelay();

        StanzaRelayBroker stanzaRelayBroker = new StanzaRelayBroker();
        stanzaRelayBroker.setInternalRelay(internalStanzaRelay);
        stanzaRelayBroker.setExternalRelay(externalStanzaRelay);

        ServerFeatures serverFeatures = new ServerFeatures();
        serverFeatures.setAuthenticationMethods(saslMechanisms);

        serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity, stanzaRelayBroker, serverFeatures, dictionaries, resourceRegistry);
        serverRuntimeContext.setStorageProviderRegistry(storageProviderRegistry);
        serverRuntimeContext.setTlsContextFactory(tlsContextFactory);

        serverRuntimeContext.addModule(new ServiceDiscoveryModule());
        serverRuntimeContext.addModule(new RosterModule());

        stanzaRelayBroker.setServerRuntimeContext(serverRuntimeContext);
        internalStanzaRelay.setServerRuntimeContext(serverRuntimeContext);

        if (endpoints.size() == 0) throw new IllegalStateException("server must have at least one endpoint");
        for (Endpoint endpoint : endpoints) {
            endpoint.setServerRuntimeContext(serverRuntimeContext);
            endpoint.start();
        }
    }

    public void stop() {
        for (Endpoint endpoint : endpoints) {
            endpoint.stop();
        }
    }

    public void addModule(Module module) {
        serverRuntimeContext.addModule(module);
    }
    
    private void addCoreDictionaries(List<NamespaceHandlerDictionary> dictionaries) {
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.starttls.StartTLSStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.sasl.SASLStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.bind.BindResourceDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.session.SessionStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.JabberIQAuthDictionary());
    }
}
