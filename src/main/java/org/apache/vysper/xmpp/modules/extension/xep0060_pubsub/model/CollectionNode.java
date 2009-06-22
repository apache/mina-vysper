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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.NullPersistenceManager;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubPersistenceManager;

/**
 * A collection node is a special pubsub node containing only other nodes. Either more CollectionNodes or
 * LeafNodes or both.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class CollectionNode {

    // the persistence manager for storing and retrieving node-info
    protected PubSubPersistenceManager storage;

    /**
     * Initializes the CollectionNode
     */
    public CollectionNode() {
        storage = new NullPersistenceManager();
    }

    /**
     * Search for a given node via its JID. We currently only support a flat hierachy, so no
     * other node types are available ATM.
     * 
     * @return the LeafNode for the JID
     */
    public LeafNode find(Entity jid) {
        return storage.findNode(jid);
    }

    /**
     * Creates a new node under the given JID.
     * 
     * @param jid the JID of the new node.
     * @return the newly created LeafNode.
     * @throws DuplicateNodeException if the JID is already taken.
     */
    public LeafNode createNode(Entity jid) throws DuplicateNodeException {
        if(storage.containsNode(jid)) {
            throw new DuplicateNodeException(jid.getFullQualifiedName() + " already present");
        }

        LeafNode node = new LeafNode(jid);
        node.setPersistenceManager(storage);

        storage.storeNode(jid, node);

        return node;
    }

    /**
     * Change the persistency manager.
     * 
     * @param persistenceManager the new persitency manager.
     */
    public void setPersistenceManager(PubSubPersistenceManager persistenceManager) {
        storage = persistenceManager;
    }
}
