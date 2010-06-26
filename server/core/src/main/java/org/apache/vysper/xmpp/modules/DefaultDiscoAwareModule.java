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
package org.apache.vysper.xmpp.modules;

import static org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * typically your module will publish/announce services it provides through service discovery. in this cases it is
 * recommended to subclass from here.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
abstract public class DefaultDiscoAwareModule extends DefaultModule {

    final Logger logger = LoggerFactory.getLogger(DefaultDiscoAwareModule.class);

    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        ServerRuntimeContextService service = serverRuntimeContext
                .getServerRuntimeContextService(SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
        if (service == null) {
            logger.error("cannot register disco request listeners: no registry service found");
            return;
        }

        ServiceDiscoveryRequestListenerRegistry requestListenerRegistry = (ServiceDiscoveryRequestListenerRegistry) service;

        List<InfoRequestListener> infoRequestListeners = new ArrayList<InfoRequestListener>();
        addInfoRequestListeners(infoRequestListeners);
        for (InfoRequestListener infoRequestListener : infoRequestListeners) {
            if (infoRequestListener == null)
                continue;
            requestListenerRegistry.addInfoRequestListener(infoRequestListener);
        }

        List<ServerInfoRequestListener> serverInfoRequestListeners = new ArrayList<ServerInfoRequestListener>();
        addServerInfoRequestListeners(serverInfoRequestListeners);
        for (ServerInfoRequestListener serverInfoRequestListener : serverInfoRequestListeners) {
            if (serverInfoRequestListener == null)
                continue;
            requestListenerRegistry.addServerInfoRequestListener(serverInfoRequestListener);
        }

        List<ComponentInfoRequestListener> componentInfoRequestListeners = new ArrayList<ComponentInfoRequestListener>();
        addComponentInfoRequestListeners(componentInfoRequestListeners);
        for (ComponentInfoRequestListener componentInfoRequestListener : componentInfoRequestListeners) {
            if (componentInfoRequestListener == null)
                continue;
            requestListenerRegistry.addComponentInfoRequestListener(componentInfoRequestListener);
        }

        List<ItemRequestListener> itemRequestListeners = new ArrayList<ItemRequestListener>();
        addItemRequestListeners(itemRequestListeners);
        for (ItemRequestListener itemRequestListener : itemRequestListeners) {
            if (itemRequestListener == null)
                continue;
            requestListenerRegistry.addItemRequestListener(itemRequestListener);
        }

    }

    protected void addInfoRequestListeners(List<InfoRequestListener> infoRequestListeners) {
        // emtpy default implementation
    }

    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        // emtpy default implementation
    }

    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        // emtpy default implementation
    }

    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        // emtpy default implementation
    }
}
