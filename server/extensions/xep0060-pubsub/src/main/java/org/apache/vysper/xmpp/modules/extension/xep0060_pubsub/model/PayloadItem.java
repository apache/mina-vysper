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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model;

import java.util.Date;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;

/**
 * This class encapsulates the payload of published notifications. It stores
 * additional information (like the date published) etc.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PayloadItem implements Comparable<PayloadItem> {

    // publisher of the item
    protected Entity publisher;

    // the id of the item
    protected String itemID;

    // tha payload
    protected XMLElement payload;

    // the date-time we received the payload
    protected Date publishedDate;

    /**
     * Create new PayloadItem with the XML encoded payload and its itemID.
     * @param publisher
     * @param payload
     * @param itemID
     */
    public PayloadItem(Entity publisher, XMLElement payload, String itemID) {
        this.publisher = publisher;
        this.payload = payload;
        this.itemID = itemID;
        this.publishedDate = new Date(); // initialized with the current date/time
    }

    /**
     * Compares the two publishedDates.
     */
    public int compareTo(PayloadItem o) {
        return this.publishedDate.compareTo(o.publishedDate);
    }

    /**
     * @return the itemID of the item.
     */
    public String getItemID() {
        return itemID;
    }

}
