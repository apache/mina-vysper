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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

public class PubsubCreateButtonListener implements ActionListener {

    private String nodeID;

    private PubsubClientModel parent;

    private JFrame frame;

    public PubsubCreateButtonListener(JFrame frame, PubsubClientModel parent) {
        this.parent = parent;
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        if (askForNodeName()) {
            try {
                parent.getPubsubMgr().createNode(nodeID);
                System.out.println("Node created " + nodeID);
                parent.refresh();
            } catch (XMPPException | SmackException.NoResponseException | SmackException.NotConnectedException | InterruptedException e1) {
                System.err.println("Couldn't create node " + nodeID);
                e1.printStackTrace();
            }
        }
    }

    private boolean askForNodeName() {
        JLabel nodeLab = new JLabel("Node ID");
        JTextField nodeTxt = new JTextField();
        nodeLab.setLabelFor(nodeTxt);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(nodeLab);
        panel.add(nodeTxt);

        int answer = JOptionPane.showOptionDialog(frame, panel, "Create new node", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new String[] { "OK", "Cancel" }, null);

        if (answer != 0) {
            return false;
        }

        this.nodeID = nodeTxt.getText();
        return true;
    }

}
