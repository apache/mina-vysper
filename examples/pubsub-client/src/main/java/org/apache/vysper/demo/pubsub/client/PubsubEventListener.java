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
package org.apache.vysper.demo.pubsub.client;

import javax.swing.DefaultListModel;

import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

public class PubsubEventListener implements ItemEventListener<PayloadItem<SimplePayload>> {
    private PubsubClientModel parent;

    public PubsubEventListener(PubsubClientModel parent) {
        this.parent = parent;
    }

    public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> e) {
        DefaultListModel lm = parent.getListModel(e.getNodeId());
        System.out.println("Got something from " + e.getNodeId());
        
        for (PayloadItem<SimplePayload> i : e.getItems()) {
            lm.add(0, i.getPayload().toXML(null)); //alwasy add to the top
        }
    }
}
