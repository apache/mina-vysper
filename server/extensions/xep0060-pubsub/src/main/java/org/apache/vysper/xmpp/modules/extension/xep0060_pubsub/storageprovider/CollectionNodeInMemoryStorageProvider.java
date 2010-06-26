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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider;

import java.util.HashMap;
import java.util.Map;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;

/**
 * This storage provider keeps all objects in memory and looses its content when
 * removed from memory. This is the default storage provider for collection nodes.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class CollectionNodeInMemoryStorageProvider implements CollectionNodeStorageProvider {

    // Map to store the nodes, access via JID
    protected Map<String, LeafNode> nodes;

    /**
     * Initialize the storage provider.
     */
    public CollectionNodeInMemoryStorageProvider() {
        nodes = new HashMap<String, LeafNode>();
    }

    /**
     * Search for a LeafNode via its name.
     */
    public LeafNode findNode(String nodeName) {
        return nodes.get(nodeName);
    }

    /**
     * Check whether an LeafNode with the given node is already known.
     */
    public boolean containsNode(String nodeName) {
        return nodes.containsKey(nodeName);
    }

    /**
     * Add the given LeafNode with the given JID to the storage.
     * An existing node with the same JID will be replaced.
     */
    public void storeNode(LeafNode node) {
        nodes.put(node.getName(), node);
    }

    /**
     * Walk through all known nodes, calling visit on each.
     *
     * @param nv the visitor to be called.
     */
    public void acceptNodes(NodeVisitor nv) {
        for (String node : nodes.keySet()) {
            nv.visit(nodes.get(node));
        }
    }

    /**
     * The in-memory storage provider does not need initialization beyond creating the objects.
     */
    public void initialize() {
        // empty
    }

    /**
     * Delete the node specified by nodeName.
     */
    public void deleteNode(String nodeName) {
        LeafNode n = findNode(nodeName);
        n.delete();
        this.nodes.remove(nodeName);
    }
}
