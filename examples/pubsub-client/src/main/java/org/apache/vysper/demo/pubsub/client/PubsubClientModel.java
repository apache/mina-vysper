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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListModel;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.Affiliation;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;

public class PubsubClientModel {
    private Map<String, DefaultListModel> nodeMessages = new TreeMap<String, DefaultListModel>();

    private PubsubTableModel tableModel = new PubsubTableModel();

    private PubsubEventListener pel = new PubsubEventListener(this);

    private XMPPConnection connection;

    private PubSubManager pubsubMgr;

    private String username;

    private String hostname;

    private String password;

    private String jid;

    private String selectedNode;

    public PubSubManager getPubsubMgr() {
        return pubsubMgr;
    }

    public PubsubTableModel getTableModel() {
        return tableModel;
    }

    public String getJID() {
        return jid;
    }

    private void discoverAffiliations(Map<String, PubsubNode> lookup) throws XMPPException {
        List<Affiliation> lAffiliations = pubsubMgr.getAffiliations();
        for (Affiliation affiliation : lAffiliations) {
            System.out.print(affiliation.getType());
            System.out.print(" of ");
            System.out.println(affiliation.getNodeId());

            PubsubNode n = lookup.get(affiliation.getNodeId());
            n.setOwnership(affiliation.getType().toString().equals("owner"));
        }
    }

    private void discoverSubscriptions(Map<String, PubsubNode> lookup) throws XMPPException {
        List<Subscription> lSubscriptions = pubsubMgr.getSubscriptions();
        for (Subscription subscription : lSubscriptions) {
            System.out.print(subscription.getState());
            System.out.print(" at ");
            System.out.println(subscription.getNode());

            PubsubNode n = lookup.get(subscription.getNode());
            if (n != null) {
                n.setSubscribed(subscription.getState().toString().equals("subscribed"));
            }
        }
    }

    private void discoverNodes(Map<String, PubsubNode> lookup) throws XMPPException {
        DiscoverItems di = pubsubMgr.discoverNodes(null);
        Iterator<Item> iIt = di.getItems();
        while (iIt.hasNext()) {
            Item i = iIt.next();
            System.out.println("Adding " + i.getNode());

            PubsubNode n = new PubsubNode(i.getNode());
            if (n != null) {
                lookup.put(i.getNode(), n);
            }
        }
    }

    public boolean login() {
        XMPPConnection.DEBUG_ENABLED = false;
        try {
            connection = connect(username, password, hostname);
        } catch (XMPPException e) {
            System.err.println("Login failed for user " + username);
            e.printStackTrace();
            return false;
        }

        pubsubMgr = new PubSubManager(connection, "pubsub.vysper.org");
        return true;
    }

    private XMPPConnection connect(String username, String password, String host) throws XMPPException {
        XMPPConnection connection = new XMPPConnection(host);
        connection.connect();
        connection.login(username, password);
        return connection;
    }

    public void refresh() {
        Map<String, PubsubNode> lookup = new HashMap<String, PubsubNode>();

        try {
            discoverNodes(lookup);
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        try {
            discoverSubscriptions(lookup);
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        try {
            discoverAffiliations(lookup);
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        tableModel.clear();
        tableModel.startBulkAdd();
        for (PubsubNode n : lookup.values()) {
            tableModel.bulkAddRow(n);

            try {
                Node node = pubsubMgr.getNode(n.getNode());
                node.removeItemEventListener(pel); // remove the listener in cases we already know the node
                node.addItemEventListener(pel); // add the listener for events
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
        tableModel.endBulkAdd();
    }

    public void logout() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setJID(String jid) {
        this.jid = jid;
    }

    public void deselectNode() {
        this.selectedNode = null;
    }

    public void selectNode(String selectedNode) {
        this.selectedNode = selectedNode;
    }

    public String getSelectedNode() {
        return this.selectedNode;
    }

    public DefaultListModel getListModel(String nodeID) {
        if (!nodeMessages.containsKey(nodeID)) {
            DefaultListModel dlm = new DefaultListModel();
            nodeMessages.put(nodeID, dlm);
        }
        return nodeMessages.get(nodeID);
    }

    public boolean isOwner(String nodeID) {
        return tableModel.isOwner(nodeID);
    }
}
