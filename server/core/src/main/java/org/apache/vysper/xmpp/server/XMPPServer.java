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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.logstanzas.LogStorageProvider;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.authentication.SASLMechanism;
import org.apache.vysper.xmpp.cryptography.NonCheckingX509TrustManagerFactory;
import org.apache.vysper.xmpp.cryptography.InputStreamBasedTLSContextFactory;
import org.apache.vysper.xmpp.cryptography.TrustManagerFactory;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelayBroker;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringExternalInboundStanzaRelay;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringInternalInboundStanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.OfflineStorageProvider;
import org.apache.vysper.xmpp.modules.roster.RosterModule;
import org.apache.vysper.xmpp.modules.servicediscovery.ServiceDiscoveryModule;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.state.resourcebinding.DefaultResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

import static java.util.Optional.ofNullable;

/**
 * this class is able to boot a standalone XMPP server.
 * <code>
 * XMPPServer server = new XMPPServer("vysper.org");
 *
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
    
    private StanzaRelayBroker stanzaRelayBroker;
    
    private StanzaProcessor stanzaProcessor;
    
    private InputStream tlsCertificate;

    private String tlsCertificatePassword;

    private String tlsKeyStoreType;

    private final List<Endpoint> endpoints = new ArrayList<Endpoint>();

    private final List<Module> initialModules = new ArrayList<Module>();
 
    private int maxInternalRelayThreads = -1;
    
    private int maxExternalRelayThreads = -1;

    public XMPPServer(String domain) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("server domain cannot be blank, empty or NULL");
        }
        if (!domain.equals(domain.toLowerCase())) {
            throw new IllegalArgumentException("server domain must be given in all lower-case letters, but was: " + domain);
        }
        try {
            EntityImpl.parse(domain);
        } catch (EntityFormatException e) {
            throw new IllegalArgumentException("server domain must be a valid domain name, but was: " + domain);
        }
        this.serverDomain = domain;

        // default list of SASL mechanisms
        saslMechanisms.add(new Plain());
        
        // add default modules
        initialModules.add(new ServiceDiscoveryModule());
        initialModules.add(new RosterModule());
    }

    public void setSASLMechanisms(List<SASLMechanism> validMechanisms) {
        saslMechanisms.addAll(validMechanisms);
    }

    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry) {
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public void setStanzaProcessor(StanzaProcessor stanzaProcessor) {
        this.stanzaProcessor = stanzaProcessor;
    }

    public void setTLSCertificateInfo(File certificate, String password) throws FileNotFoundException {
        tlsCertificate = new FileInputStream(certificate);
        tlsCertificatePassword = password;
    }

    public void setTLSCertificateInfo(InputStream certificate, String password) {
    	setTLSCertificateInfo(certificate, password, null);
    }

    public void setTLSCertificateInfo(InputStream certificate, String password, String keyStoreType) {
    	tlsCertificate = certificate;
    	tlsCertificatePassword = password;
    	tlsKeyStoreType = keyStoreType;
    }

    public void setMaxInternalRelayThreads(int maxInternalRelayThreads) {
        this.maxInternalRelayThreads = maxInternalRelayThreads;
    }

    public void setMaxExternalRelayThreads(int maxExternalRelayThreads) {
        this.maxExternalRelayThreads = maxExternalRelayThreads;
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void start() throws Exception {

        ServerFeatures serverFeatures = createServerFeatures();
        serverFeatures.setAuthenticationMethods(saslMechanisms);

        TrustManagerFactory trustManagerFactory = null; // default, check certificates strictly
        if (!serverFeatures.isCheckingFederationServerCertificates()) {
            // switch to accepting *any* certificate 
            trustManagerFactory = new NonCheckingX509TrustManagerFactory();
        }

        if (StringUtils.isNotEmpty(tlsCertificatePassword) && tlsCertificate == null) {
            throw new IllegalStateException("no TLS certificate loaded for the configured password");
        }
        InputStreamBasedTLSContextFactory tlsContextFactory = new InputStreamBasedTLSContextFactory(tlsCertificate);
        tlsContextFactory.setPassword(tlsCertificatePassword);
        tlsContextFactory.setTrustManagerFactory(trustManagerFactory);
        if(tlsKeyStoreType != null) {
        	tlsContextFactory.setKeyStoreType(tlsKeyStoreType);
        }

        List<HandlerDictionary> dictionaries = new ArrayList<HandlerDictionary>();
        addCoreDictionaries(dictionaries);

        ResourceRegistry resourceRegistry = new DefaultResourceRegistry();

        EntityImpl serverEntity = new EntityImpl(null, serverDomain, null);

        AccountManagement accountManagement = (AccountManagement) storageProviderRegistry
                .retrieve(AccountManagement.class);
        OfflineStanzaReceiver offlineReceiver = (OfflineStanzaReceiver) storageProviderRegistry.retrieve(OfflineStorageProvider.class);
        DeliveringInternalInboundStanzaRelay internalStanzaRelay = new DeliveringInternalInboundStanzaRelay(serverEntity,
                resourceRegistry, accountManagement,offlineReceiver);
        DeliveringExternalInboundStanzaRelay externalStanzaRelay = new DeliveringExternalInboundStanzaRelay();
        
        if (maxInternalRelayThreads >= 0) internalStanzaRelay.setMaxThreadCount(maxInternalRelayThreads);
        if (maxExternalRelayThreads >= 0) externalStanzaRelay.setMaxThreadCount(maxExternalRelayThreads);

        stanzaRelayBroker = new StanzaRelayBroker();
        stanzaRelayBroker.setInternalRelay(internalStanzaRelay);
        stanzaRelayBroker.setExternalRelay(externalStanzaRelay);

        serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity, stanzaRelayBroker, serverFeatures,
                dictionaries, resourceRegistry);
        serverRuntimeContext.setStorageProviderRegistry(storageProviderRegistry);
        serverRuntimeContext.setTlsContextFactory(tlsContextFactory);
        ofNullable(stanzaProcessor).ifPresent(serverRuntimeContext::setStanzaProcessor);

        for(Module module : initialModules) {
            serverRuntimeContext.addModule(module);
        }

        stanzaRelayBroker.setServerRuntimeContext(serverRuntimeContext);
        internalStanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        externalStanzaRelay.setServerRuntimeContext(serverRuntimeContext);

        final LogStorageProvider logStorageProvider =
                (LogStorageProvider) this.storageProviderRegistry.retrieve(LogStorageProvider.class);
        if (logStorageProvider != null) internalStanzaRelay.setLogStorageProvider(logStorageProvider);

        if (endpoints.size() == 0) throw new IllegalStateException("server must have at least one endpoint");
        for (Endpoint endpoint : endpoints) {
            endpoint.setServerRuntimeContext(serverRuntimeContext);
            endpoint.start();
        }
    }

    protected ServerFeatures createServerFeatures() {
        return new ServerFeatures();
    }

    public void stop() {
        for (Endpoint endpoint : endpoints) {
            endpoint.stop();
        }
        
        for(Module module : serverRuntimeContext.getModules()) {
            try {
                module.close();
            } catch(RuntimeException e) {
                // ignore
            }
        }
        
        stanzaRelayBroker.stop();
        serverRuntimeContext.getServerConnectorRegistry().close();
    }

    public void addModule(Module module) {
        if(serverRuntimeContext != null) {
            serverRuntimeContext.addModule(module);
        } else {
            initialModules.add(module);
        }
    }

    private void addCoreDictionaries(List<HandlerDictionary> dictionaries) {
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.starttls.StartTLSStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.sasl.SASLStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.bind.BindResourceDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.session.SessionStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.JabberIQAuthDictionary());
    }
    
    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }
}
