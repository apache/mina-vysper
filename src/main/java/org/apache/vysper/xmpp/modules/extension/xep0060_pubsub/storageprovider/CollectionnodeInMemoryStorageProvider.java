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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;

/**
 * This storage provider keeps all objects in memory and looses its content when
 * removed from memory. This is the default storage provider for collection nodes.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class CollectionnodeInMemoryStorageProvider implements CollectionNodeStorageProvider {

    // Map to store the nodes, access via JID
    protected Map<Entity, LeafNode> nodes;

    /**
     * Initialize the storage provider.
     */
    public CollectionnodeInMemoryStorageProvider() {
        nodes = new HashMap<Entity, LeafNode>();
    }

    /**
     * Search for a LeafNode via its JID.
     */
    public LeafNode findNode(Entity jid) {
        return nodes.get(jid);
    }

    /**
     * Check whether an LeafNode with the given JID is known.
     */
    public boolean containsNode(Entity jid) {
        return nodes.containsKey(jid);
    }

    /**
     * Add the given LeafNode with the given JID to the storage.
     * An existing ode with the same JID will be replaced.
     */
    public void storeNode(Entity jid, LeafNode node) {
        nodes.put(jid, node);
    }
}
