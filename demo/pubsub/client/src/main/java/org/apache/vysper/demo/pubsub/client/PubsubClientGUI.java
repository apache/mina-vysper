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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.Affiliation;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;

/**
 * A simple demo application for the pubsub module of Vysper. It allows to lookup
 * the nodes, subscribe, unsubscribe, create new nodes and finally to publish and
 * receive published items.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubClientGUI implements Runnable, TableModelListener {
    private PubsubTableModel dtm = new PubsubTableModel();
    private XMPPConnection connection;
    private PubSubManager pubsubMgr;
    private String jid = "user1@vysper.org";

    private void createAndShowGUI() {
        setUpLookAndFeel();

        JFrame frame = new JFrame("Vysper Publish/Subscribe Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();

        JTable nodeTable = new JTable(dtm);
        JScrollPane scrollPane = new JScrollPane(nodeTable);
        nodeTable.setFillsViewportHeight(true);
        dtm.addTableModelListener(this);

        JButton create = new JButton("Create");

        JButton delete = new JButton("Delete");

        frame.add(panel);

        panel.add(scrollPane);
        panel.add(create);
        panel.add(delete);

        frame.pack();//setSize(200,200);
        frame.setVisible(true);
    }

    private void setUpLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch(Exception e) {
            // well then... no change
        }
    }

    public static void main(String[] args) {
        PubsubClientGUI ex1 = new PubsubClientGUI();
        SwingUtilities.invokeLater(ex1);
    }

    private void logout() {
        connection.disconnect();
    }

    @Override
    public void run() {
        createAndShowGUI();
        registerShutDownHook();

        login();
        refresh();
    }

    private void registerShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logout();
            }
        });
    }

    private void refresh() {
        Map<String, PubsubNode> lookup = new HashMap<String, PubsubNode>();

        try {
            discoverNodes(lookup);
            discoverSubscriptions(lookup);
            discoverAffiliations(lookup);
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        for(PubsubNode n : lookup.values()) {
            dtm.addRow(n);
        }
    }

    private void discoverAffiliations(Map<String, PubsubNode> lookup) throws XMPPException {
        List<Affiliation> lAffiliations = pubsubMgr.getAffiliations();
        for(Affiliation affiliation : lAffiliations) {
            System.out.print(affiliation.getNodeId());
            System.out.print(": ");
            System.out.println(affiliation.getType());

            PubsubNode n = lookup.get(affiliation.getNodeId());
            n.setOwnership(affiliation.getType().toString().equals("owner"));
        }
    }

    private void discoverSubscriptions(Map<String, PubsubNode> lookup) throws XMPPException {
        List<Subscription> lSubscriptions = pubsubMgr.getSubscriptions();
        for(Subscription subscription : lSubscriptions) {
            System.out.print(subscription.getNode());
            System.out.print(": ");
            System.out.println(subscription.getState());

            PubsubNode n = lookup.get(subscription.getNode());
            if(n != null) {
                n.setSubscribed(subscription.getState().toString().equals("subscribed"));
            }
        }
    }

    private void discoverNodes(Map<String, PubsubNode> lookup) throws XMPPException {
        DiscoverItems di = pubsubMgr.discoverNodes();
        Iterator<Item> iIt = di.getItems();
        while(iIt.hasNext()) {
            Item i = iIt.next();
            System.out.println("Adding " + i.getNode());

            PubsubNode n = new PubsubNode(i.getNode());
            if(n != null) {
                lookup.put(i.getNode(), n);
            }
        }
    }

    private void login() {
        try {
            connection = connect("user1", "password1", "localhost");
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        pubsubMgr = new PubSubManager(connection);
    }

    private XMPPConnection connect(String username, String password, String host) throws XMPPException {
        XMPPConnection connection = new XMPPConnection(host);
        connection.connect();
        connection.login(username, password);
        return connection;
    }

    @Override
    public void tableChanged(TableModelEvent event) {
        try {
            Boolean sub = (Boolean)dtm.getValueAt(event.getFirstRow(), event.getColumn());
            String nodeName = (String)dtm.getValueAt(event.getFirstRow(), 0);

            Node node = pubsubMgr.getNode(nodeName);

            if(sub.booleanValue()) { // contains the new value (soll)
                node.subscribe(jid);
            } else {
                node.unsubscribe(jid);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
