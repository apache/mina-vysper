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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;


/**
 * A chat room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Room implements InfoRequestListener, ItemRequestListener {

    private EnumSet<RoomType> roomTypes;

    private Entity jid;
    private String name;
    
    public Room(Entity jid, String name, RoomType... types) {
        this.jid = jid;
        this.name = name;
        
        EnumSet<RoomType> potentialTypes;
        if(types != null && types.length > 0) {
            potentialTypes = EnumSet.copyOf(Arrays.asList(types));

            // make sure the list does not contain antonyms
            RoomType.validateAntonyms(potentialTypes);
        } else {
            potentialTypes = EnumSet.noneOf(RoomType.class);
        }
        
        // complement with default types
        this.roomTypes = RoomType.complement(potentialTypes);            
    }

    public Entity getJID() {
        return jid;
    }
    
    public String getName() {
        return name;
    }

    public List<InfoElement> getInfosFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));
        
        for(RoomType type : roomTypes) {
            infoElements.add(new Feature(type.getDiscoName()));            
        }
        
        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request)
            throws ServiceDiscoveryRequestException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
