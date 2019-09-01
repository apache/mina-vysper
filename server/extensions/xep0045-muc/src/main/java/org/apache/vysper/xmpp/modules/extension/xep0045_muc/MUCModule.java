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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCIqAdminHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045
 * Multi-user chat</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCModule extends DefaultDiscoAwareModule
        implements Component, ComponentInfoRequestListener, ItemRequestListener {

    private final MUCFeatures mucFeatures = new MUCFeatures();

    private final String subdomain;

    private final Conference conference;

    private Entity fullDomain;

    private final Logger logger = LoggerFactory.getLogger(MUCModule.class);

    private ServerRuntimeContext serverRuntimeContext;

    public MUCModule(String subdomain) {
        this(subdomain, null);
    }

    public MUCModule() {
        this(null, null);
    }

    public MUCModule(String subdomain, Conference conference) {
        if (subdomain == null)
            subdomain = "chat";
        this.subdomain = subdomain;
        if (conference == null)
            conference = new Conference("Conference", mucFeatures);
        this.conference = conference;
    }

    /**
     * Initializes the MUC module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        this.serverRuntimeContext = serverRuntimeContext;

        fullDomain = EntityUtils.createComponentDomain(subdomain, serverRuntimeContext);

        RoomStorageProvider roomStorageProvider = serverRuntimeContext.getStorageProvider(RoomStorageProvider.class);
        OccupantStorageProvider occupantStorageProvider = serverRuntimeContext
                .getStorageProvider(OccupantStorageProvider.class);

        if (roomStorageProvider == null) {
            logger.warn("No room storage provider found, using the default (in memory)");
        } else {
            conference.setRoomStorageProvider(roomStorageProvider);
        }

        if (occupantStorageProvider == null) {
            logger.warn("No occupant storage provider found, using the default (in memory)");
        } else {
            conference.setOccupantStorageProvider(occupantStorageProvider);
        }

        this.conference.initialize();
    }

    @Override
    public String getName() {
        return "XEP-0045 Multi-user chat";
    }

    @Override
    public String getVersion() {
        return "1.24";
    }

    public MUCFeatures getFeatures() {
        return mucFeatures;
    }

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }

    public List<InfoElement> getComponentInfosFor(InfoRequest request, StanzaBroker stanzaBroker)
            throws ServiceDiscoveryRequestException {
        if (!EntityUtils.isAddressingServer(fullDomain, request.getTo()))
            return null;

        if (request.getTo().getNode() == null) {
            List<InfoElement> serverInfos = conference.getServerInfosFor(request);
            return serverInfos;
        } else {
            // might be an items request on a room
            Room room = conference.findRoom(request.getTo().getBareJID());
            if (room == null)
                return null;

            if (request.getTo().getResource() != null) {
                // request for an occupant
                Occupant occupant = room.findOccupantByNick(request.getTo().getResource());
                // request for occupant, relay
                if (occupant != null) {
                    relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO,
                            stanzaBroker);
                }
                return null;
            } else {
                return room.getInfosFor(request);
            }
        }
    }

    @Override
    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        componentInfoRequestListeners.add(this);
    }

    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener}
     * interface. Makes this modules available via disco#items and returns the
     * associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest, StanzaBroker)
     */
    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker)
            throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        if (to.getNode() == null) {
            // react on request send to server domain or this subdomain, but not to others
            if (fullDomain.equals(to)) {
                List<Item> conferenceItems = conference.getItemsFor(request, stanzaBroker);
                return conferenceItems;
            } else if (serverRuntimeContext.getServerEntity().equals(to)) {
                List<Item> componentItem = new ArrayList<Item>();
                componentItem.add(new Item(fullDomain));
                return componentItem;
            }
            return null;
        } else if (EntityUtils.isAddressingServer(fullDomain, to)) {
            // might be an items request on a room
            Room room = conference.findRoom(to.getBareJID());
            if (room != null) {
                if (to.getResource() != null) {
                    // request for an occupant
                    Occupant occupant = room.findOccupantByNick(to.getResource());
                    // request for occupant, relay
                    if (occupant != null) {
                        relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS,
                                stanzaBroker);
                    }
                } else {
                    return room.getItemsFor(request, stanzaBroker);
                }
            }
        }
        return null;
    }

    private void relayDiscoStanza(Entity receiver, InfoRequest request, String ns, StanzaBroker stanzaBroker) {
        StanzaBuilder builder = StanzaBuilder.createIQStanza(request.getFrom(), receiver, IQStanzaType.GET,
                request.getID());
        builder.startInnerElement("query", ns);
        if (request.getNode() != null) {
            builder.addAttribute("node", request.getNode());
        }

        try {
            stanzaBroker.write(receiver, builder.build(), IgnoreFailureStrategy.INSTANCE);
        } catch (DeliveryException e) {
            // ignore
        }

    }

    public String getSubdomain() {
        return subdomain;
    }

    @Override
    public List<StanzaHandler> getComponentHandlers(Entity fullDomain) {
        List<StanzaHandler> handlers = new ArrayList<>();
        handlers.add(new MUCPresenceHandler(conference));
        handlers.add(new MUCMessageHandler(conference, fullDomain));
        handlers.add(new MUCIqAdminHandler(conference));
        return handlers;
    }

    @Override
    public List<NamespaceHandlerDictionary> getComponentHandlerDictionnaries(Entity fullDomain) {
        return Collections.emptyList();
    }

}
