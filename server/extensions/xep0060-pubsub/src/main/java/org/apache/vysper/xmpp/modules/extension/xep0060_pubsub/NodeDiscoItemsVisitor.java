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
import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.PayloadItem;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;

/**
 * This visitor is used to collect all items of a node for disco#items requests.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
@SpecCompliant(spec = "xep-0060", section = "5.5", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class NodeDiscoItemsVisitor implements ItemVisitor {

    // list to hold the items (ordered)
    List<PayloadItem> itemList = new ArrayList<PayloadItem>();

    // The JID of the pubsub service
    Entity serviceJID;

    public NodeDiscoItemsVisitor(Entity serviceJID) {
        this.serviceJID = serviceJID;
    }

    /**
     * Gets called with each itemID and payload of a node. Builds the answer
     * for disco#items requests to a node.
     * 
     * @see org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.ItemVisitor#visit(java.lang.String, org.apache.vysper.xmpp.xmlfragment.XMLElement)
     */
    public void visit(String itemID, PayloadItem payload) {
        itemList.add(payload);
    }

    /**
     * @return the ordered list of items.
     */
    public List<Item> getItemList() {
        List<Item> discoItems = new ArrayList<Item>();
        Collections.sort(itemList);
        for (PayloadItem pi : itemList) {
            discoItems.add(new Item(serviceJID, pi.getItemID(), null));
        }
        return discoItems;
    }

}
