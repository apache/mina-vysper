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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeStorageProvider;

/**
 * A collection node is a special pubsub node containing only other nodes. Either more CollectionNodes or
 * LeafNodes or both.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class CollectionNode {

    // the storage provider for storing and retrieving node-info
    protected CollectionNodeStorageProvider collectionNodeStorage;

    /**
     * Search for a given node via its name. We currently only support a flat hierarchy, so no
     * other node types are available ATM.
     * 
     * @return the LeafNode for the JID
     */
    public LeafNode find(String name) {
        return collectionNodeStorage.findNode(name);
    }

    /**
     * Adds the given leaf node to the collection. If the node is already present
     * (or another node with the same name), it will be replaced with the new one.
     * @param node
     */
    public void add(LeafNode node) {
        collectionNodeStorage.storeNode(node);
    }

    /**
     * Visit all nodes.
     *
     * @param nv the visitor to be called.
     */
    public void acceptNodes(NodeVisitor nv) {
        collectionNodeStorage.acceptNodes(nv);
    }

    /**
     * Deletes a node.
     *
     * @param nodeName the node to delete
     */
    public void deleteNode(String nodeName) {
        this.collectionNodeStorage.deleteNode(nodeName);
    }

    /**
     * Changes the storage provider for the collection node.
     * 
     * @param collectionNodeStorageProvider
     */
    public void setCollectionNodeStorageProvider(CollectionNodeStorageProvider collectionNodeStorageProvider) {
        this.collectionNodeStorage = collectionNodeStorageProvider;
    }
}
