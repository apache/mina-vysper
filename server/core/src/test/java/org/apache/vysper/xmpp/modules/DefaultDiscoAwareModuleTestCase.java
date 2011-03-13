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

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


/**
 */
public class DefaultDiscoAwareModuleTestCase extends Mockito {

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);
    private ServiceDiscoveryRequestListenerRegistry registry = mock(ServiceDiscoveryRequestListenerRegistry.class);
    
    private InfoRequestListener infoRequestListener1 = mock(InfoRequestListener.class);
    private InfoRequestListener infoRequestListener2 = mock(InfoRequestListener.class);
    private List<InfoRequestListener> infoRequestListeners = Arrays.asList(infoRequestListener1, null, infoRequestListener2);
    
    private ServerInfoRequestListener serverInfoRequestListener1 = mock(ServerInfoRequestListener.class);
    private ServerInfoRequestListener serverInfoRequestListener2 = mock(ServerInfoRequestListener.class);
    private List<ServerInfoRequestListener> serverInfoRequestListeners = Arrays.asList(serverInfoRequestListener1, null, serverInfoRequestListener2);
    
    private ComponentInfoRequestListener componentInfoRequestListener1 = mock(ComponentInfoRequestListener.class);
    private ComponentInfoRequestListener componentInfoRequestListener2 = mock(ComponentInfoRequestListener.class);
    private List<ComponentInfoRequestListener> componentInfoRequestListeners = Arrays.asList(componentInfoRequestListener1, null, componentInfoRequestListener2);
    
    private ItemRequestListener itemRequestListener1 = mock(ItemRequestListener.class);
    private ItemRequestListener itemRequestListener2 = mock(ItemRequestListener.class);
    private List<ItemRequestListener> itemRequestListeners = Arrays.asList(itemRequestListener1, null, itemRequestListener2);
    
    private DefaultDiscoAwareModule module = new DefaultDiscoAwareModule() {
        @Override
        public String getVersion() {
            return null;
        }
        
        @Override
        public String getName() {
            return null;
        }

        @Override
        protected void addInfoRequestListeners(List<InfoRequestListener> listeners) {
            listeners.addAll(infoRequestListeners);
        }

        @Override
        protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> listeners) {
            listeners.addAll(serverInfoRequestListeners);
        }

        @Override
        protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> listeners) {
            listeners.addAll(componentInfoRequestListeners);
        }

        @Override
        protected void addItemRequestListeners(List<ItemRequestListener> listeners) {
            listeners.addAll(itemRequestListeners);
        }
    };
    
    @Before
    public void before() {
        when(serverRuntimeContext.getServerRuntimeContextService(ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY))
            .thenReturn(registry);
    }
    
    @Test
    public void initialize() throws DeliveryException {
        module.initialize(serverRuntimeContext);
        
        verify(registry).addInfoRequestListener(infoRequestListener1);
        verify(registry).addInfoRequestListener(infoRequestListener2);

        verify(registry).addServerInfoRequestListener(serverInfoRequestListener1);
        verify(registry).addServerInfoRequestListener(serverInfoRequestListener2);
        
        verify(registry).addComponentInfoRequestListener(componentInfoRequestListener1);
        verify(registry).addComponentInfoRequestListener(componentInfoRequestListener2);
        
        verify(registry).addItemRequestListener(itemRequestListener1);
        verify(registry).addItemRequestListener(itemRequestListener2);
    }

    
}
