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
package org.apache.vysper.xmpp.modules.servicediscovery.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;

/**
 * on an item or info requests, calls all related listeners and collects what
 * they have to add to the response. compiles the responded infos and items.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServiceCollector implements ServiceDiscoveryRequestListenerRegistry {

    private static final Feature DEFAULT_FEATURE = new Feature(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);

    protected final List<InfoRequestListener> infoRequestListeners = new ArrayList<InfoRequestListener>();

    protected final List<ServerInfoRequestListener> serverInfoRequestListeners = new ArrayList<ServerInfoRequestListener>();

    protected final List<ComponentInfoRequestListener> componentInfoRequestListeners = new ArrayList<ComponentInfoRequestListener>();

    protected final List<ItemRequestListener> itemRequestListeners = new ArrayList<ItemRequestListener>();

    public void addInfoRequestListener(InfoRequestListener infoRequestListener) {
        infoRequestListeners.add(infoRequestListener);
    }

    public void addServerInfoRequestListener(ServerInfoRequestListener infoRequestListener) {
        serverInfoRequestListeners.add(infoRequestListener);
    }

    public void addComponentInfoRequestListener(ComponentInfoRequestListener infoRequestListener) {
        componentInfoRequestListeners.add(infoRequestListener);
    }

    public void addItemRequestListener(ItemRequestListener itemRequestListener) {
        itemRequestListeners.add(itemRequestListener);
    }

    /**
     * collect all server feature and identity info from the listeners
     */
    public List<InfoElement> processServerInfoRequest(InfoRequest infoRequest) throws ServiceDiscoveryRequestException {
        // sorted structure, to place all <feature/> after <identity/>
        List<InfoElement> elements = new ArrayList<InfoElement>();
        elements.add(DEFAULT_FEATURE);
        for (ServerInfoRequestListener serverInfoRequestListener : serverInfoRequestListeners) {
            List<InfoElement> elementList = null;
            try {
                elementList = serverInfoRequestListener.getServerInfosFor(infoRequest);
            } catch (ServiceDiscoveryRequestException abortion) {
                throw abortion;
            } catch (Throwable e) {
                continue;
            }
            if (elementList != null)
                elements.addAll(elementList);
        }
        Collections.sort(elements, new ElementPartitioningComparator());
        return elements;
    }

    public List<InfoElement> processComponentInfoRequest(InfoRequest infoRequest, StanzaBroker stanzaBroker)
            throws ServiceDiscoveryRequestException {
        // sorted structure, to place all <feature/> after <identity/>
        List<InfoElement> elements = new ArrayList<InfoElement>();
        for (ComponentInfoRequestListener componentInfoRequestListener : componentInfoRequestListeners) {
            List<InfoElement> elementList = null;
            try {
                elementList = componentInfoRequestListener.getComponentInfosFor(infoRequest, stanzaBroker);
            } catch (ServiceDiscoveryRequestException abortion) {
                throw abortion;
            } catch (Throwable e) {
                continue;
            }
            if (elementList != null)
                elements.addAll(elementList);
        }
        Collections.sort(elements, new ElementPartitioningComparator());
        return elements;
    }

    /**
     * collect all non-server feature and identity info from the listeners
     */
    public List<InfoElement> processInfoRequest(InfoRequest infoRequest) throws ServiceDiscoveryRequestException {
        // sorted structure, to place all <feature/> after <identity/>
        List<InfoElement> elements = new ArrayList<InfoElement>();
        elements.add(DEFAULT_FEATURE);
        for (InfoRequestListener infoRequestListener : infoRequestListeners) {
            List<InfoElement> elementList = null;
            try {
                elementList = infoRequestListener.getInfosFor(infoRequest);
            } catch (ServiceDiscoveryRequestException abortion) {
                throw abortion;
            } catch (Throwable e) {
                continue;
            }
            if (elementList != null)
                elements.addAll(elementList);
        }
        Collections.sort(elements, new ElementPartitioningComparator());
        return elements;
    }

    /**
     * collect all item info from the listeners
     */
    public List<Item> processItemRequest(InfoRequest infoRequest, StanzaBroker stanzaBroker)
            throws ServiceDiscoveryRequestException {
        List<Item> elements = new ArrayList<Item>();
        for (ItemRequestListener itemRequestListener : itemRequestListeners) {
            List<Item> elementList;
            try {
                elementList = itemRequestListener.getItemsFor(infoRequest, stanzaBroker);
            } catch (ServiceDiscoveryRequestException abortion) {
                throw abortion;
            } catch (Throwable e) {
                continue;
            }
            if (elementList != null)
                elements.addAll(elementList);
        }
        return elements;
    }

    public String getServiceName() {
        return SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY;
    }

    static class ElementPartitioningComparator implements Comparator<InfoElement> {
        public int compare(InfoElement o1, InfoElement o2) {
            return o1.getElementClassId().compareTo(o2.getElementClassId());
        }
    }
}
