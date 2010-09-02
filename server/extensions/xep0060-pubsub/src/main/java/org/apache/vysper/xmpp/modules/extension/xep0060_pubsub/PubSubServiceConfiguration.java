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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;

/**
 * This class represents the configuration for the publish/subscribe service.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubSubServiceConfiguration {
    private Entity serverJID;

    private CollectionNode rootNode;

    private CollectionNodeStorageProvider collectionNodeStorageProvider;

    private LeafNodeStorageProvider leafNodeStorageProvider;

    /**
     * Creates a new configuration object containing at least the root collection node.
     * 
     * @param root
     */
    public PubSubServiceConfiguration(CollectionNode root) {
        this.rootNode = root;
        this.leafNodeStorageProvider = new LeafNodeInMemoryStorageProvider();
        this.collectionNodeStorageProvider = new CollectionNodeInMemoryStorageProvider();
        this.initialize();
    }

    /**
     * Sets the JID of the pubsub service.
     * this can either be the server's own domain, or - more common - a subdomain of the server like 
     * "pubsub.server.tld"
     * 
     * @param serverJID
     */
    public void setDomainJID(Entity serverJID) {
        this.serverJID = serverJID;
    }

    /**
     * @return the domain of the pubsub component.
     */
    public Entity getDomainJID() {
        return serverJID;
    }

    /**
     * @return the root collection node.
     */
    public CollectionNode getRootNode() {
        return rootNode;
    }

    /**
     * Set the storage provider for leaf nodes.
     * @param leafNodeStorageProvider
     */
    public void setLeafNodeStorageProvider(LeafNodeStorageProvider leafNodeStorageProvider) {
        this.leafNodeStorageProvider = leafNodeStorageProvider;
    }

    /**
     * Return the storage provider for leaf nodes.
     * @return
     */
    public LeafNodeStorageProvider getLeafNodeStorageProvider() {
        return leafNodeStorageProvider;
    }

    /**
     * Set the collection node storage provider.
     * @param collectionNodeStorageProvider
     */
    public void setCollectionNodeStorageProvider(CollectionNodeStorageProvider collectionNodeStorageProvider) {
        this.collectionNodeStorageProvider = collectionNodeStorageProvider;
    }

    /**
     * Return the leaf node storage provider.
     * @return
     */
    public CollectionNodeStorageProvider getCollectionNodeStorageProvider() {
        return collectionNodeStorageProvider;
    }

    /**
     * To be called after the storage providers are set or changed.
     */
    public void initialize() {
        rootNode.setCollectionNodeStorageProvider(this.collectionNodeStorageProvider);
    }

}
