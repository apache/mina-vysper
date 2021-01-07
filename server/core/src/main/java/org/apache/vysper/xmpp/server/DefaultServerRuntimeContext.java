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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.vysper.event.EventBus;
import org.apache.vysper.event.SimpleEventBus;
import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authentication.UserAuthentication;
import org.apache.vysper.xmpp.cryptography.TLSContextFactory;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ModuleRegistry;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.ProtocolWorker;
import org.apache.vysper.xmpp.protocol.SimpleStanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.protocol.StanzaHandlerLookup;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.components.AlterableComponentRegistry;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.SimpleComponentRegistry;
import org.apache.vysper.xmpp.server.s2s.DefaultXMPPServerConnectorRegistry;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.presence.LatestPresenceCache;
import org.apache.vysper.xmpp.state.presence.SimplePresenceCache;
import org.apache.vysper.xmpp.state.resourcebinding.DefaultResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultServerRuntimeContext implements InternalServerRuntimeContext, ModuleRegistry {

    private final Logger logger = LoggerFactory.getLogger(DefaultServerRuntimeContext.class);

    // basic internal data structures and configuration...

    /**
     * directory where all available processors for incoming stanzas are located
     */
    private final StanzaHandlerLookup stanzaHandlerLookup;

    /**
     * the 'domain' the server is directly serving for
     */
    private final Entity serverEntity;

    /**
     * feature configuration
     */
    private final ServerFeatures serverFeatures;

    /**
     * the Secure Socket Engine in use
     */
    private SSLContext sslContext = null;

    /**
     * generates unique session ids
     */
    private final UUIDGenerator sessionIdGenerator = new JVMBuiltinUUIDGenerator();

    // basic services the server is using...

    /**
     * 'output stream': receives stanzas issued by a session, which are going to
     * other sessions/servers
     */
    private final StanzaRelay stanzaRelay;

    /**
     * administrate and query resources and sessions
     */
    private ResourceRegistry resourceRegistry;

    /**
     * holds the latest presence stanza for a resource
     */
    private LatestPresenceCache presenceCache = new SimplePresenceCache();

    private final XMPPServerConnectorRegistry serverConnectorRegistry;

    /**
     * holds the storage services
     */
    private StorageProviderRegistry storageProviderRegistry = new OpenStorageProviderRegistry();

    /**
     * collection of all other services, which are mostly add-ons to the minimal
     * setup
     */
    private final Map<String, ServerRuntimeContextService> serverRuntimeContextServiceMap = new HashMap<String, ServerRuntimeContextService>();

    private List<Module> modules = new ArrayList<>();

    /**
     * map of all registered components, index by the subdomain they are registered
     * for
     */
    private final AlterableComponentRegistry componentRegistry;

    private final SimpleEventBus eventBus;

    private final ComponentStanzaProcessorFactory componentStanzaProcessorFactory;

    private final List<StanzaHandlerInterceptor> stanzaHandlerInterceptors = new ArrayList<>();

    public DefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay, StanzaProcessor stanzaProcessor,
            AlterableComponentRegistry componentRegistry, ResourceRegistry resourceRegistry,
            ServerFeatures serverFeatures, List<HandlerDictionary> dictionaries,
            OfflineStanzaReceiver offlineStanzaReceiver) {
        this.serverEntity = serverEntity;
        this.stanzaRelay = stanzaRelay;
        this.componentRegistry = requireNonNull(componentRegistry);
        StanzaHandlerExecutorFactory simpleStanzaHandlerExecutorFactory = new SimpleStanzaHandlerExecutorFactory(
                stanzaRelay, offlineStanzaReceiver);
        this.serverConnectorRegistry = new DefaultXMPPServerConnectorRegistry(this, simpleStanzaHandlerExecutorFactory,
                stanzaProcessor);
        this.stanzaHandlerLookup = new StanzaHandlerLookup(this);
        this.eventBus = new SimpleEventBus();
        this.serverFeatures = serverFeatures;
        this.resourceRegistry = resourceRegistry;
        this.componentStanzaProcessorFactory = new ComponentStanzaProcessorFactory(simpleStanzaHandlerExecutorFactory);

        addDictionaries(dictionaries);
    }

    public DefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay,
            StorageProviderRegistry storageProviderRegistry) {
        this(serverEntity, stanzaRelay);
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public DefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay) {
        this(serverEntity, stanzaRelay,
                new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(stanzaRelay, null)),
                new SimpleComponentRegistry(serverEntity), new DefaultResourceRegistry(), new ServerFeatures(),
                Collections.emptyList(),
                null);
    }

    /**
     * change the presence cache implementation. this is a setter intended to be
     * used at initialisation time. (thus, this method is not part of
     * ServerRuntimeContext.
     */
    public void setPresenceCache(LatestPresenceCache presenceCache) {
        this.presenceCache = presenceCache;
    }

    @Override
    public StanzaHandler getHandler(Stanza stanza) {
        return stanzaHandlerLookup.getHandler(stanza);
    }

    @Override
    public String getNextSessionId() {
        return sessionIdGenerator.create();
    }

    @Override
    public Entity getServerEntity() {
        return serverEntity;
    }

    @Override
    public String getDefaultXMLLang() {
        return "en"; // TODO must be configurable as of RFC3920
    }

    public StanzaRelay getStanzaRelay() {
        return stanzaRelay;
    }

    @Override
    public ServerFeatures getServerFeatures() {
        return serverFeatures;
    }

    @Override
    public XMPPServerConnectorRegistry getServerConnectorRegistry() {
        return serverConnectorRegistry;
    }

    public void addDictionary(HandlerDictionary namespaceHandlerDictionary) {
        stanzaHandlerLookup.addDictionary(namespaceHandlerDictionary);
    }

    protected void addDictionaries(List<HandlerDictionary> dictionaries) {
        for (HandlerDictionary dictionary : dictionaries) {
            addDictionary(dictionary);
        }
    }

    public void setTlsContextFactory(TLSContextFactory tlsContextFactory) {
        try {
            sslContext = tlsContextFactory.getSSLContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * @deprecated use {@link #getStorageProvider(Class)} with
     *             {@link UserAuthentication}.class instead
     */
    @Override
    public UserAuthentication getUserAuthentication() {
        return storageProviderRegistry.retrieve(UserAuthentication.class);
    }

    @Override
    public ResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    @Override
    public LatestPresenceCache getPresenceCache() {
        return presenceCache;
    }

    /**
     * add a runtime service. makes the service dynamically discoverable at runtime.
     * 
     * @param service
     */
    @Override
    public void registerServerRuntimeContextService(ServerRuntimeContextService service) {
        if (service == null)
            throw new IllegalStateException("service must not be null");
        if (serverRuntimeContextServiceMap.get(service.getServiceName()) != null) {
            throw new IllegalStateException("service already registered: " + service.getServiceName());
        }
        serverRuntimeContextServiceMap.put(service.getServiceName(), service);
    }

    /**
     * retrieves a previously registered runtime context service. The RosterManager
     * is a good example of such a service. This allows for modules, extensions and
     * other services to discover their dependencies at runtime.
     * 
     * @see org.apache.vysper.xmpp.server.DefaultServerRuntimeContext#getStorageProvider(Class)
     * @param name
     * @return
     */
    @Override
    public ServerRuntimeContextService getServerRuntimeContextService(String name) {
        return serverRuntimeContextServiceMap.get(name);
    }

    /**
     * adds a whole set of storage providers at once to the system.
     * 
     * @param storageProviderRegistry
     */
    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry) {
        logger.info("replacing the storage provider registry with "
                + storageProviderRegistry.getClass().getCanonicalName());
        this.storageProviderRegistry = storageProviderRegistry;
    }

    /**
     * retrieves a particular storage provider.
     * 
     * @param clazz
     * @return
     */
    @Override
    public <T extends StorageProvider> T getStorageProvider(Class<T> clazz) {
        return storageProviderRegistry.retrieve(clazz);
    }

    /**
     * adds and initializes a list of Modules. A module extends the server's
     * functionality by adding an XMPP extension ('XEP') to it. (More) Modules can
     * be added at runtime. This approach has an advantage over adding modules one
     * by one, in that it allows for a better dependency management: all modules
     * from the list to first discover each other before initialize() get's called
     * for every one of them.
     * 
     * @see org.apache.vysper.xmpp.server.DefaultServerRuntimeContext#registerServerRuntimeContextService(org.apache.vysper.xmpp.modules.ServerRuntimeContextService)
     * @see org.apache.vysper.xmpp.server.DefaultServerRuntimeContext#getServerRuntimeContextService(String)
     * @see org.apache.vysper.xmpp.modules.Module
     * @param modules
     *            List of modules
     */
    @Override
    public void addModules(List<Module> modules) {
        for (Module module : modules) {
            addModuleInternal(module);
        }
        for (Module module : modules) {
            module.initialize(this);
        }
    }

    /**
     * adds and initializes a single Module. a module extends the server's
     * functionality by adding an XMPP extension ('XEP') to it.
     * 
     * @see org.apache.vysper.xmpp.modules.Module
     * @see DefaultServerRuntimeContext#addModules(java.util.List) for adding a
     *      number of modules at once
     * @param module
     */
    @Override
    public void addModule(Module module) {
        addModuleInternal(module);
        module.initialize(this);
    }

    protected void addModuleInternal(Module module) {

        logger.info("adding module... {} ({})", module.getName(), module.getVersion());

        List<ServerRuntimeContextService> serviceList = module.getServerServices();
        if (serviceList != null) {
            for (ServerRuntimeContextService serverRuntimeContextService : serviceList) {
                registerServerRuntimeContextService(serverRuntimeContextService);

                // if a storage service, also register there
                if (serverRuntimeContextService instanceof StorageProvider) {
                    StorageProvider storageProvider = (StorageProvider) serverRuntimeContextService;
                    storageProviderRegistry.add(storageProvider);
                }
            }
        }

        List<HandlerDictionary> handlerDictionaryList = module.getHandlerDictionaries();
        if (handlerDictionaryList != null) {

            for (HandlerDictionary handlerDictionary : handlerDictionaryList) {
                addDictionary(handlerDictionary);
            }

        }

        module.getEventListenerDictionary().ifPresent(eventBus::addDictionary);

        if (module instanceof Component) {
            registerComponent((Component) module);
        }

        stanzaHandlerInterceptors.addAll(module.getStanzaHandlerInterceptors());

        modules.add(module);
    }

    @Override
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    @Override
    public <T> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz))
                return (T) module;
        }
        return null;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors() {
        return stanzaHandlerInterceptors;
    }

    @Override
    public void registerComponent(Component component) {
        componentRegistry.registerComponent(componentStanzaProcessorFactory, component);
    }

    @Override
    public boolean hasComponentStanzaProcessor(Entity entity) {
        return componentRegistry.getComponentStanzaProcessor(entity) != null;
    }

}
