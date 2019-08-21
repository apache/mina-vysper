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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

public class PubsubDeleteButtonListener implements ActionListener {

    private PubsubClientModel parent;

    private JFrame frame;

    public PubsubDeleteButtonListener(JFrame frame, PubsubClientModel parent) {
        this.parent = parent;
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        String nodeID = parent.getSelectedNode();
        if (nodeID != null && askForSure()) {
            try {
                parent.getPubsubMgr().deleteNode(nodeID);
                System.out.println("Node deleted: " + nodeID);
                parent.refresh();
            } catch (XMPPException | SmackException.NoResponseException | SmackException.NotConnectedException | InterruptedException e1) {
                System.err.println("Couldn't delete node " + nodeID);
                e1.printStackTrace();
            }
        }
    }

    private boolean askForSure() {
        JLabel nodeLab = new JLabel("Node ID");
        JTextField nodeTxt = new JTextField();
        nodeLab.setLabelFor(nodeTxt);

        int answer = JOptionPane.showConfirmDialog(frame, "The node and all associated data will be lost!",
                "Delete node?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (answer != JOptionPane.OK_OPTION) {
            return false;
        }
        return true;
    }

}
