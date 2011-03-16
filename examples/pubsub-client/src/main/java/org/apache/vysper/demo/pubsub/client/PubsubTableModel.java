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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -3788690749950634883L;

    private final String[] columnNames = new String[] { "Node", "Subscribed", "Owner" };

    private List<PubsubNode> nodes = new ArrayList<PubsubNode>();

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return nodes.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        PubsubNode n = nodes.get(rowIndex);

        if (n == null)
            return null;

        if (columnIndex == 0)
            return n.getNode();
        if (columnIndex == 1)
            return n.getSubscribed();
        if (columnIndex == 2)
            return n.getOwnership();

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class getColumnClass(int c) {
        if (c == 0)
            return String.class;
        return Boolean.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == 1) {
            return true;
        }
        return false;
    }

    public void addRow(PubsubNode node) {
        this.nodes.add(node);
        Collections.sort(this.nodes);
        fireTableRowsInserted(getRowCount() - 1, getColumnCount() - 1);
    }

    public void bulkAddRow(PubsubNode node) {
        this.nodes.add(node);
    }

    public void deleteRow(int rowIndex) {
        this.nodes.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        PubsubNode n = this.nodes.get(rowIndex);
        if (columnIndex == 1)
            n.setSubscribed((Boolean) aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void clear() {
        int rowCount = getRowCount();
        nodes.clear();
        fireTableRowsDeleted(0, rowCount);
    }

    public void startBulkAdd() {
        // for consistency
    }

    public void endBulkAdd() {
        Collections.sort(this.nodes);
        fireTableRowsInserted(0, this.getRowCount() - 1);
    }

    public boolean isOwner(String nodeID) {
        for (PubsubNode n : nodes) {
            if (n.getNode().equals(nodeID)) {
                return n.getOwnership();
            }
        }
        return false;
    }
}
