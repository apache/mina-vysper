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

import org.jxmpp.stringprep.XmppStringprepException;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple demo application for the pubsub module of Vysper. It allows to lookup
 * the nodes, subscribe, unsubscribe, create new nodes and finally to publish and
 * receive published items.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubClientGUI implements Runnable, ListSelectionListener {
    private JFrame frame;

    private JButton delete;

    private JButton open;

    private PubsubClientModel pcm = new PubsubClientModel();

    private void createAndShowGUI() {
        setUpLookAndFeel();

        PubsubTableModel tableModel = pcm.getTableModel();

        frame = new JFrame("Vysper Publish/Subscribe Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTable nodeTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(nodeTable);
        //nodeTable.setFillsViewportHeight(true);
        nodeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = nodeTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        tableModel.addTableModelListener(new PubsubTableModelListener(pcm));

        JButton create = new JButton("Create node");
        create.setActionCommand("create");
        create.addActionListener(new PubsubCreateButtonListener(frame, pcm));

        open = new JButton("Open node");
        open.setActionCommand("open");
        open.addActionListener(new PubsubOpenButtonListener(pcm));
        disableOpenButton();

        delete = new JButton("Delete node");
        delete.setActionCommand("delete");
        delete.addActionListener(new PubsubDeleteButtonListener(frame, pcm));
        delete.setEnabled(false);

        JButton refresh = new JButton("Refresh");
        refresh.setActionCommand("refresh");
        refresh.addActionListener(new PubsubRefreshButtonListener(pcm));

        JPanel buttons = new JPanel();
        buttons.add(create);
        buttons.add(open);
        buttons.add(delete);
        buttons.add(refresh);

        frame.add(panel);

        panel.add(scrollPane, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }

    private void setUpLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // well then... no change
        }
    }

    public static void main(String[] args) {
        PubsubClientGUI ex1 = new PubsubClientGUI();
        SwingUtilities.invokeLater(ex1);
    }

    public void run() {
        createAndShowGUI();
        registerShutDownHook();

        try {
            login();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return;
        }
        pcm.refresh();
    }

    public void login() throws XmppStringprepException {
        do {
            askForCredentials();
        } while (pcm.login() == false);
    }

    private void registerShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                pcm.logout();
            }
        });
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
        panel.setLayout(new GridLayout(4, 2));
        panel.add(jidLab);
        panel.add(jidTxt);
        panel.add(usernameLab);
        panel.add(usernameTxt);
        panel.add(hostLab);
        panel.add(hostTxt);
        panel.add(passwordLab);
        panel.add(passwordTxt);

        int answer = JOptionPane.showOptionDialog(frame, panel, "Login", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new String[] { "Login", "Exit" }, "Login");

        if (answer != 0) {
            System.exit(0);
        }

        pcm.setUsername(usernameTxt.getText());
        pcm.setHostname(hostTxt.getText());
        pcm.setPassword(passwordTxt.getText());
        pcm.setJID(jidTxt.getText());
    }

    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (e.getValueIsAdjusting()) {
            if (lsm.isSelectionEmpty()) {
                disableOpenButton();
                disableDeleteButton();
            } else {
                changeOpenButton();
                changeDeleteButton(e);
            }
        }
    }

    private void disableOpenButton() {
        // disable open button
        open.setEnabled(false);
    }

    private void disableDeleteButton() {
        // disable delete button
        delete.setEnabled(false);
        pcm.deselectNode();
    }

    private void changeOpenButton() {
        open.setEnabled(true);
    }

    private void changeDeleteButton(ListSelectionEvent e) {
        // store the node and enable delete button
        PubsubTableModel tableModel = pcm.getTableModel();

        int idx = getNewSelectionIndex(e, tableModel);

        String selectedNode = (String) tableModel.getValueAt(idx, 0);
        pcm.selectNode(selectedNode);
        Boolean owner = (Boolean) tableModel.getValueAt(idx, 2);
        if (owner != null && owner == Boolean.TRUE) { //owner
            delete.setEnabled(true);
        } else {
            delete.setEnabled(false);
        }
    }

    private int getNewSelectionIndex(ListSelectionEvent e, PubsubTableModel tableModel) {
        int idx = e.getFirstIndex(); // check which one is the right index (the new one)
        String selectedNode = (String) tableModel.getValueAt(idx, 0);
        if (pcm.getSelectedNode() != null && pcm.getSelectedNode().equals(selectedNode)) {
            idx = e.getLastIndex();

        }
        return idx;
    }

}
