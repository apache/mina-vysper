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

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class CollectionNodeTestCase extends TestCase  {

    protected CollectionNode collection;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        collection = new CollectionNode();
        collection.initialize(new EntityImpl(null, "pubsub.vysper.org", null));
    }

    public void testCreateNode() throws Exception {
        Entity jid = new EntityImpl(null, "pubsub.vysper.org", "test1");
        LeafNode test1 = collection.createNode(jid);
        assertNotNull(test1);
    }

    public void testCreateNodeTwice() {
        Entity jid = new EntityImpl(null, "pubsub.vysper.org", "test1");
        try {
            collection.createNode(jid);
            collection.createNode(jid);
            fail();
        } catch(DuplicateNodeException e) {
            // ok
        }
    }

    public void testInsertFind() throws Exception {
        Entity jid = new EntityImpl(null, "pubsub.vysper.org", "test1");
        LeafNode insertedNode = collection.createNode(jid);
        LeafNode foundNode = collection.find(jid);
        assertEquals(insertedNode, foundNode);
    }

    public void testFindNone() {
        Entity jid = new EntityImpl(null, "pubsub.vysper.org", "test1");
        assertNull(collection.find(jid));
    }

}
