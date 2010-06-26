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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class PubsubTableModelListener implements TableModelListener {
    private PubsubClientModel parent;

    public PubsubTableModelListener(PubsubClientModel parent) {
        this.parent = parent;
    }

    public void tableChanged(TableModelEvent event) {
        PubsubTableModel dtm = parent.getTableModel();
        PubSubManager pubsubMgr = parent.getPubsubMgr();
        String jid = parent.getJID();

        if (event.getType() == TableModelEvent.UPDATE) {
            try {
                Boolean sub = (Boolean) dtm.getValueAt(event.getFirstRow(), event.getColumn());
                String nodeName = (String) dtm.getValueAt(event.getFirstRow(), 0);

                Node node = pubsubMgr.getNode(nodeName);

                if (sub.booleanValue()) { // contains the new value (soll)
                    node.subscribe(jid);
                    System.out.println(jid + " subscribed to " + node.getId());
                } else {
                    node.unsubscribe(jid);
                    System.out.println(jid + " unsubscribed from " + node.getId());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
