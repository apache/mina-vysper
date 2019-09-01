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

import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.vysper.event.EventBus;
import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authentication.UserAuthentication;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.presence.LatestPresenceCache;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 * provides each session with server-global data
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface ServerRuntimeContext {
    StanzaHandler getHandler(Stanza stanza);

    String getNextSessionId();

    Entity getServerEntity();

    String getDefaultXMLLang();

    ServerFeatures getServerFeatures();

    SSLContext getSslContext();

    UserAuthentication getUserAuthentication();

    ResourceRegistry getResourceRegistry();

    LatestPresenceCache getPresenceCache();

    void registerServerRuntimeContextService(ServerRuntimeContextService service);

    ServerRuntimeContextService getServerRuntimeContextService(String name);

    <T extends StorageProvider> T getStorageProvider(Class<T> clazz);

    void registerComponent(Component component);

    boolean hasComponentStanzaProcessor(Entity entity);

    List<Module> getModules();

    <T> T getModule(Class<T> clazz);

    void addModule(Module module);

    EventBus getEventBus();

    List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors();
}
