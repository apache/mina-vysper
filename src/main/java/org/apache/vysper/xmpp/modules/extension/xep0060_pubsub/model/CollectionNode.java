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

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionnodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;

/**
 * A collection node is a special pubsub node containing only other nodes. Either more CollectionNodes or
 * LeafNodes or both.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class CollectionNode {

    // the storage provider for storing and retrieving node-info
    protected CollectionNodeStorageProvider collectionNodeStorage;
    // the storage provider for leaf nodes
    protected LeafNodeStorageProvider leafNodeStorage;

    /**
     * Creates a new CollectionNode, leaves the nodeJID uninitialized.
     */
    public CollectionNode() {
        initStorageProviders();
    }

    /**
     * Initializes the default in-memory storage providers.
     */
    private void initStorageProviders() {
        collectionNodeStorage = new CollectionnodeInMemoryStorageProvider();
        leafNodeStorage = new LeafNodeInMemoryStorageProvider();
    }

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
     * Creates a new node under the given JID.
     * 
     * @param serverJID the JID of the server this node is associated with.
     * @param nodeName the unique node-name of the node (its key)
     * @param givenName the free-text name of the node
     * @return the newly created LeafNode.
     * @throws DuplicateNodeException if the JID is already taken.
     */
    public LeafNode createNode(Entity serverJID, String nodeName, String givenName) throws DuplicateNodeException {
        LeafNode node = new LeafNode(serverJID, nodeName, givenName);
        node.setPersistenceManager(leafNodeStorage);

        if(collectionNodeStorage.containsNode(nodeName)) {
            throw new DuplicateNodeException(serverJID.getFullQualifiedName() + nodeName + " " + givenName + " already present");
        }

        collectionNodeStorage.storeNode(node);

        return node;
    }
    
    /**
     * Convenience method to create a node without a free-text name (optional).
     * 
     * @param serverJID the JID of the server new node lies on.
     * @param nodeName the unique name of the node
     * @return the newly create LeafNode
     * @throws DuplicateNodeException
     */
    public LeafNode createNode(Entity serverJID, String nodeName) throws DuplicateNodeException {
        return this.createNode(serverJID, nodeName, null);
    }

    /**
     * Change the storage provider to be used for the collection nodes.
     * 
     * @param storageProvider the new storage provider.
     */
    public void setCollectionNodeStorageProvider(CollectionNodeStorageProvider storageProvider) {
        this.collectionNodeStorage = storageProvider;
    }

    /**
     * Change the storage provider to be used for the leaf nodes.
     * 
     * @param storageProvider the new storage provider.
     */
    public void setLeafNodeStorageProvider(LeafNodeStorageProvider storageProvider) {
        this.leafNodeStorage = storageProvider;
    }

    public void acceptNodes(NodeVisitor nv) {
        collectionNodeStorage.acceptNodes(nv);
    }

    /**
     * Called after setting the storage providers to do its own initialization tasks.
     */
    public void initialize() {
        this.collectionNodeStorage.initialize();
        this.leafNodeStorage.initialize();
    }
}
