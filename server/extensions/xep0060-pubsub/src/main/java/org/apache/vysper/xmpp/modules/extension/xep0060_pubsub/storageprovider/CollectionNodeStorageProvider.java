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

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NodeVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;

/**
 * This interface defines all methods a StorageProvider has to offer to be suitable
 * for a CollectionNode in the pubsub-module.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public interface CollectionNodeStorageProvider extends StorageProvider {

    /**
     * Retrieve a node via its JID
     * @param nodeName the name of the node we're searching for
     * @return the LeafNode if found, null otherwise.
     */
    public LeafNode findNode(String nodeName);

    /**
     * Checks whether the collection node contains a node with a certain JID.
     * @param nodeName the node we're searching for
     * @return true if the JID corresponds to a known node.
     */
    public boolean containsNode(String nodeName);

    /**
     * Stores a node under the specified JID.
     * @param node the LeafNode to be stored.
     */
    public void storeNode(LeafNode node);

    /**
     * Call the NodeVisitor for each node of the given collection node.
     * 
     * @param nv the visitior to be called.
     */
    public void acceptNodes(NodeVisitor nv);

    /**
     * Call to do some preliminary tasks after the module has been configured.
     */
    public void initialize();

    /**
     * Delete the node with name nodeName.
     * @param nodeName the name of the node to delete.
     */
    public void deleteNode(String nodeName);
}
