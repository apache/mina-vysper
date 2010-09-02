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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class ServiceDiscoItemsVisitor implements NodeVisitor {

    private List<Item> itemList = new ArrayList<Item>();

    private PubSubServiceConfiguration serviceConfiguration;

    public ServiceDiscoItemsVisitor(PubSubServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    /**
     * Prepare the node-list for the disco#items response.
     * 
     * @see org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeVisitor#visit(org.apache.vysper.xmpp.addressing.Entity, org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode)
     */
    public void visit(LeafNode ln) {
        this.itemList.add(new Item(serviceConfiguration.getDomainJID(), ln.getTitle(), ln.getName()));
    }

    /**
     * Return a list of items to be embedded in the disco#items response.
     * To be called after visit!
     */
    public List<Item> getNodeItemList() {
        return itemList;
    }

}
