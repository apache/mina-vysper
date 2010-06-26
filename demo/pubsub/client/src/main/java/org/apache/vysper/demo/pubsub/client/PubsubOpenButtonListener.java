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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class PubsubOpenButtonListener implements ActionListener {

    private PubsubClientModel parent;

    public PubsubOpenButtonListener(PubsubClientModel parent) {
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        String nodeID = parent.getSelectedNode();
        if (nodeID != null) {
            createAndShowGUI(nodeID);
        }
    }

    private void createAndShowGUI(String nodeID) {
        ListModel lm = parent.getListModel(nodeID);

        JFrame frame = new JFrame("Events from " + nodeID);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JList eventList = new JList(lm);
        JScrollPane scrollPane = new JScrollPane(eventList);
        frame.add(panel);

        panel.add(scrollPane, BorderLayout.NORTH);

        JTextField messageTxt = createOwnerControls(nodeID, panel);

        frame.pack();
        frame.setVisible(true);

        setFocus(messageTxt);
    }

    private void setFocus(JTextField messageTxt) {
        if (messageTxt != null) {
            messageTxt.requestFocusInWindow();
        }
    }

    private JTextField createOwnerControls(String nodeID, JPanel panel) {
        boolean owner = parent.isOwner(nodeID);
        JTextField messageTxt = null;
        if (owner) {
            messageTxt = new JTextField(20);

            JButton publish = new JButton("Publish");
            publish.setActionCommand("publish");
            publish.addActionListener(new PubsubPublishButtonListener(nodeID, messageTxt, parent));

            JPanel buttons = new JPanel();
            buttons.add(messageTxt);
            buttons.add(publish);

            panel.add(buttons, BorderLayout.SOUTH);
        }
        return messageTxt;
    }
}