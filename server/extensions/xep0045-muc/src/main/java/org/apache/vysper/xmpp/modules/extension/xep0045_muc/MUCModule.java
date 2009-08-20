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

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SubdomainHandlerDictionary;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener, InfoRequestListener, ItemRequestListener {

    private Conference conference;
    private Entity domain;
    
    private final Logger logger = LoggerFactory.getLogger(MUCModule.class);
    private ServerRuntimeContext serverRuntimeContext;
    
    public MUCModule(Entity domain) {
        this(domain, new Conference("Conference"));
    }
    
    public MUCModule(Entity domain, Conference conference) {
        this.domain = domain;
        this.conference = conference;
    }
    
    public MUCModule(String domain) throws EntityFormatException {
        this(EntityImpl.parse(domain));
    }
    
    public MUCModule(String domain, Conference conference) throws EntityFormatException {
        this(EntityImpl.parse(domain), conference);
    }

    /**
     * Initializes the MUC module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);
        
        this.serverRuntimeContext = serverRuntimeContext;
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

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        return conference.getServerInfosFor(request);
    }

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }
    
    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener} interface.
     * Makes this modules available via disco#items and returns the associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest)
     */
    public List<Item> getItemsFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        if(to.getNode() == null) {
            // items request on the component
            return conference.getItemsFor(request);
        } else {
            // might be an items request on a room
            Room room = conference.findRoom(to.getBareJID());
            if(room != null) {
                if(to.getResource() != null) {
                    // request for an occupant
                    Occupant occupant = room.findOccupantByNick(to.getResource());
                    // request for occupant, relay
                    if(occupant != null) relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
                } else {
                    return room.getItemsFor(request);
                }
            }
        }
        return null;
    }
    
    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionaries) {
        // MUC is only supported for running on a subdomain
        
        SubdomainHandlerDictionary dictionary = new SubdomainHandlerDictionary(domain);
        dictionary.register(new MUCPresenceHandler(conference));
        dictionary.register(new MUCMessageHandler(conference, domain));
        
        dictionaries.add(dictionary);
    }

    /**
     * Make this object available for disco#items requests for rooms
     */
    @Override
    protected void addInfoRequestListeners(List<InfoRequestListener> infoRequestListeners) {
        infoRequestListeners.add(this);
    }
    
    private void relayDiscoStanza(Entity receiver, InfoRequest request, String ns) {
        // TODO how to get id?
        StanzaBuilder builder = StanzaBuilder.createIQStanza(request.getFrom(), receiver, IQStanzaType.GET, request.getID());
        builder.startInnerElement("query", ns);
        if(request.getNode() != null) {
            builder.addAttribute("node", request.getNode());
        }

        try {
            serverRuntimeContext.getStanzaRelay().relay(receiver, builder.getFinalStanza(), new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
            // ignore
        }
        
    }
    
    public List<InfoElement> getInfosFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        
        if(to.getNode() != null) {
            Room room = conference.findRoom(to.getBareJID());
            if(room != null) {
                if(to.getResource() != null) {
                    Occupant occupant = room.findOccupantByNick(to.getResource());
                    // request for occupant, relay
                    if(occupant != null) relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
                } else {
                // request for room
                    return room.getInfosFor(request);
                }
            }
        }
        return null;
    }
}
