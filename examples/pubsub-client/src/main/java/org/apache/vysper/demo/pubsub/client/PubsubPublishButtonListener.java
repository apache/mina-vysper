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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
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
        if (node == null)
            return;

        String message = getMessage();

        PayloadItem<SimplePayload> item = createItem(message);

        sendItem(node, item);
    }

    private String getMessage() {
        String message = messageTxt.getText();
        cleanup();
        return message;
    }

    private void sendItem(Node node, PayloadItem<SimplePayload> item) {
        try {
            if(node instanceof LeafNode) {
                ((LeafNode)node).send(item);
            } else {
                throw new IllegalArgumentException("Can only send to leaf nodes");
            }
        } catch (XMPPException | SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e1) {
            System.err.println("Couldn't send an item to " + nodeID);
            e1.printStackTrace();
        }
    }

    private PayloadItem<SimplePayload> createItem(String message) {
        String itemId = "demoID" + System.currentTimeMillis();
        PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(itemId, new SimplePayload(ELEMENT, NAMESPACE, message));
        return item;
    }

    private Node getNode(PubSubManager pubsubMgr) {
        Node node = null;
        try {
            node = pubsubMgr.getNode(nodeID);
        } catch (XMPPException | SmackException.NoResponseException | SmackException.NotConnectedException | InterruptedException | PubSubException.NotAPubSubNodeException e1) {
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
