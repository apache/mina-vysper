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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
    private JFrame frame;
    private PubsubTableModel dtm = new PubsubTableModel();
    private XMPPConnection connection;
    private PubSubManager pubsubMgr;
    private String username;
    private String hostname;
    private String password;
    private String jid;

    private void createAndShowGUI() {
        setUpLookAndFeel();

        frame = new JFrame("Vysper Publish/Subscribe Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTable nodeTable = new JTable(dtm);
        JScrollPane scrollPane = new JScrollPane(nodeTable);
        nodeTable.setFillsViewportHeight(true);
        dtm.addTableModelListener(this);

        JButton create = new JButton("Create");

        JButton delete = new JButton("Delete");
        
        JPanel buttons = new JPanel();
        buttons.add(create);
        buttons.add(delete);

        frame.add(panel);

        panel.add(scrollPane, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.SOUTH);

        frame.pack();
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
        if(connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

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
        boolean loginOK = false;
        
        do {
            askForCredentials();
            try {
                connection = connect(username, password, hostname);
                loginOK = true;
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        } while(loginOK == false);
        
        pubsubMgr = new PubSubManager(connection);
    }

    private void askForCredentials() {
        JLabel jidLab = new JLabel("JID");
        JTextField jidTxt = new JTextField("user1@vysper.org");
        jidLab.setLabelFor(jidTxt);
        
        JLabel usernameLab = new JLabel("Username");
        JTextField usernameTxt = new JTextField("user1");
        usernameLab.setLabelFor(usernameTxt);
        
        JLabel hostLab = new JLabel("Host");
        JTextField hostTxt = new JTextField("localhost");
        hostLab.setLabelFor(hostTxt);
        
        JLabel passwordLab = new JLabel("Password");
        JTextField passwordTxt = new JPasswordField("password1");
        passwordLab.setLabelFor(passwordTxt);
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4,2));
        panel.add(jidLab);
        panel.add(jidTxt);
        panel.add(usernameLab);
        panel.add(usernameTxt);
        panel.add(hostLab);
        panel.add(hostTxt);
        panel.add(passwordLab);
        panel.add(passwordTxt);
        
        int answer = JOptionPane.showOptionDialog(frame,
                panel,
                "Login",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[] {"Login", "Exit"},
                "Login");

        if(answer != 0) {
            System.exit(0);
        }
        
        this.username = usernameTxt.getText();
        this.hostname = hostTxt.getText();
        this.password = passwordTxt.getText();
        this.jid = jidTxt.getText();
    }

    private XMPPConnection connect(String username, String password, String host) throws XMPPException {
        XMPPConnection connection = new XMPPConnection(host);
        connection.connect();
        connection.login(username, password);
        return connection;
    }

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
