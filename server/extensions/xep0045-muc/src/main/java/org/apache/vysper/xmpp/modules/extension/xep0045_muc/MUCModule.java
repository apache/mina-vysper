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
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
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
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCModule 
        extends DefaultDiscoAwareModule 
        implements Component, ComponentInfoRequestListener, ItemRequestListener {

    private String subdomain = "chat";
    private Conference conference;
    private Entity fullDomain;
    
    private final Logger logger = LoggerFactory.getLogger(MUCModule.class);
    private ServerRuntimeContext serverRuntimeContext;

    private ComponentStanzaProcessor stanzaProcessor;

    public MUCModule(String subdomain) {
        this(subdomain, new Conference("Conference"));
    }
    
    public MUCModule() {
        this.conference = new Conference("Conference");
    }
    
    public MUCModule(String subdomain, Conference conference) {
        this.subdomain = subdomain;
        this.conference = conference;
    }
        
    /**
     * Initializes the MUC module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);
        
        this.serverRuntimeContext = serverRuntimeContext;

        try {
            fullDomain = EntityImpl.parse(subdomain + "." + serverRuntimeContext.getServerEnitity().getDomain());
        } catch (EntityFormatException e) {
            throw new RuntimeException("failed to initialize MUC domain", e);
        }

        ComponentStanzaProcessor processor = new ComponentStanzaProcessor(serverRuntimeContext);
        processor.addHandler(new MUCPresenceHandler(conference));
        processor.addHandler(new MUCMessageHandler(conference, fullDomain));
        stanzaProcessor = processor;

        RoomStorageProvider roomStorageProvider = (RoomStorageProvider) serverRuntimeContext.getStorageProvider(RoomStorageProvider.class);
        OccupantStorageProvider occupantStorageProvider = (OccupantStorageProvider) serverRuntimeContext.getStorageProvider(OccupantStorageProvider.class);

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

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }
    
    
    public List<InfoElement> getComponentInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        if (fullDomain.getDomain().equals(request.getTo().getDomain())) {
            if (request.getTo().getNode() == null) {
                List<InfoElement> serverInfos = conference.getServerInfosFor(request);
                return serverInfos;
            } else {
                // might be an items request on a room
                Room room = conference.findRoom(request.getTo().getBareJID());
                if (room != null) {
                    if (request.getTo().getResource() != null) {
                        // request for an occupant
                        Occupant occupant = room.findOccupantByNick(request.getTo().getResource());
                        // request for occupant, relay
                        if (occupant != null) {
                            relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
                        }
                    } else {
                        return room.getInfosFor(request);
                    }
                }

            }
        }
        return null;
    }

    @Override
    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        componentInfoRequestListeners.add(this);
    }

    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener} interface.
     * Makes this modules available via disco#items and returns the associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest)
     */
    public List<Item> getItemsFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        if (to.getNode() == null) {
            // react on request send to server domain or this subdomain, but not to others
            if(fullDomain.equals(to)) {
                List<Item> conferenceItems = conference.getItemsFor(request);
                return conferenceItems;
            } else if (serverRuntimeContext.getServerEnitity().equals(to)) {
                List<Item> componentItem = new ArrayList<Item>();
                componentItem.add(new Item(fullDomain));
                return componentItem;
            }
            return null;
        } else if (fullDomain.getDomain().equals(to.getDomain())) {
            // might be an items request on a room
            Room room = conference.findRoom(to.getBareJID());
            if (room != null) {
                if (to.getResource() != null) {
                    // request for an occupant
                    Occupant occupant = room.findOccupantByNick(to.getResource());
                    // request for occupant, relay
                    if (occupant != null) {
                        relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
                    }
                } else {
                    return room.getItemsFor(request);
                }
            }
        }
        return null;
    }
    
    private void relayDiscoStanza(Entity receiver, InfoRequest request, String ns) {
        StanzaBuilder builder = StanzaBuilder.createIQStanza(request.getFrom(), receiver, IQStanzaType.GET, request.getID());
        builder.startInnerElement("query", ns);
        if(request.getNode() != null) {
            builder.addAttribute("node", request.getNode());
        }

        try {
            serverRuntimeContext.getStanzaRelay().relay(receiver, builder.build(), new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
            // ignore
        }
        
    }


    public String getSubdomain() {
        return subdomain;
    }

    public StanzaProcessor getStanzaProcessor() {
        return stanzaProcessor;
    }

}
