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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;

public class PubsubPublishButtonListener implements ActionListener {

    private String nodeID;
    private PubsubClientModel parent;
    private JTextField messageTxt;
    private static final String ELEMENT = "message";
    private static final String NAMESPACE = "http://mina.apache.org/vysper/demo";

    public PubsubPublishButtonListener(String nodeID, JTextField messageTxt, PubsubClientModel parent) {
        this.nodeID = nodeID;
        this.parent = parent;
        this.messageTxt = messageTxt;
    }
    
    public void actionPerformed(ActionEvent e) {
        PubSubManager pubsubMgr = parent.getPubsubMgr();
        
        Node node = getNode(pubsubMgr);
        if(node == null) return;
        
        String message = getMessage();

        Item<SimplePayload> item = createItem(message);
        
        sendItem(node, item);
    }

    private String getMessage() {
        String message = messageTxt.getText();
        cleanup();
        return message;
    }

    private void sendItem(Node node, Item<SimplePayload> item) {
        try {
            node.send(item);
        } catch (XMPPException e1) {
            System.err.println("Couldn't send an item to " + nodeID);
            e1.printStackTrace();
        }
    }

    private Item<SimplePayload> createItem(String message) {
        String itemId = "demoID"+System.currentTimeMillis();
        Item<SimplePayload> item = new Item<SimplePayload>(itemId, new SimplePayload(ELEMENT, NAMESPACE, message));
        return item;
    }

    private Node getNode(PubSubManager pubsubMgr) {
        Node node = null;
        try {
            node = pubsubMgr.getNode(nodeID);
        } catch (XMPPException e1) {
            System.err.println("Couldn't get the node object for " + nodeID);
            e1.printStackTrace();
        }
        return node;
    }

    private void cleanup() {
        messageTxt.setText("");
        messageTxt.requestFocusInWindow();
    }
}
