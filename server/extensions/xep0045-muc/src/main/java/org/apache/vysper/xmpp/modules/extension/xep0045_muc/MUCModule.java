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

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
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
import org.apache.vysper.xmpp.protocol.SubdomainHandlerDictionary;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
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
        if(request.getTo().getNode() == null) {
            // items request on the component
            return conference.getItemsFor(request);
        } else {
            // might be an items request on a room
            Room room = conference.findRoom(request.getTo());
            if(room != null) {
                return room.getItemsFor(request);
            } else {
                return null;
            }
        }
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
    
    public List<InfoElement> getInfosFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        Room room = conference.findRoom(request.getTo());
        if(room != null) {
            return room.getInfosFor(request);
        } else {
            return null;
        }
    }
}
