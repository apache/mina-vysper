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

import static java.util.Objects.requireNonNull;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.vysper.event.EventBus;
import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authentication.UserAuthentication;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchivesMock;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.presence.LatestPresenceCache;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 * @author Réda Housni Alaoui
 */
public class ServerRuntimeContextMock implements ServerRuntimeContext {
    
    private final Entity serverEntity;

    private MessageArchivesMock userMessageArchives;

    public ServerRuntimeContextMock(Entity serverEntity) {
        this.serverEntity = requireNonNull(serverEntity);
    }

    public MessageArchivesMock givenUserMessageArchives() {
        userMessageArchives = new MessageArchivesMock();
        return userMessageArchives;
    }

    @Override
    public StanzaHandler getHandler(Stanza stanza) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entity getServerEntity() {
        return serverEntity;
    }

    @Override
    public String getDefaultXMLLang() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServerFeatures getServerFeatures() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLContext getSslContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserAuthentication getUserAuthentication() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceRegistry getResourceRegistry() {
        return mock(ResourceRegistry.class);
    }

    @Override
    public LatestPresenceCache getPresenceCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerServerRuntimeContextService(ServerRuntimeContextService service) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServerRuntimeContextService getServerRuntimeContextService(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends StorageProvider> T getStorageProvider(Class<T> clazz) {
        if (MessageArchives.class.equals(clazz)) {
            return (T) userMessageArchives;
        }
        return null;
    }

    @Override
    public void registerComponent(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasComponentStanzaProcessor(Entity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Module> getModules() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getModule(Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addModule(Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventBus getEventBus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors() {
        return Collections.emptyList();
    }
}
