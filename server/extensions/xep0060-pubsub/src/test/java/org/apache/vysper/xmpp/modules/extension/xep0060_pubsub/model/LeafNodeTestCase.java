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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class LeafNodeTestCase extends TestCase {

    protected LeafNode node;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Entity nodeJID = new EntityImpl(null, "pubsub.vysper.org", null);
        PubSubServiceConfiguration serviceConfig = new PubSubServiceConfiguration(new CollectionNode());
        serviceConfig.setDomainJID(nodeJID);
        Entity creatorJID = new EntityImpl("creator", "vysper.org", null);

        node = new LeafNode(serviceConfig, "node", "Some test node", creatorJID);
    }

    public void testSubscribe() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");
        assertFalse(node.isSubscribed(me));
        node.subscribe("id1", me);
        assertTrue(node.isSubscribed(me));
        assertTrue(node.isSubscribed("id1"));
    }

    public void testCount() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");

        assertEquals(0, node.countSubscriptions(me));

        node.subscribe("id1", me);
        assertEquals(1, node.countSubscriptions(me));

        node.subscribe("id2", me);
        assertEquals(2, node.countSubscriptions(me));
    }

    public void testUnsubscribe() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");
        node.subscribe("id1", me);
        assertTrue(node.isSubscribed(me));
        boolean result = node.unsubscribe(me);
        assertTrue(result);
    }

    public void testUnsubscribeMultiSubscription() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");
        node.subscribe("id1", me);
        node.subscribe("id2", me);
        assertEquals(2, node.countSubscriptions(me));

        assertTrue(node.isSubscribed(me));
        boolean result = false;
        try {
            result = node.unsubscribe(me);
            fail();
        } catch (MultipleSubscriptionException e) {
            // good
        }
        assertEquals(2, node.countSubscriptions(me));

        result = node.unsubscribe("id1", me);
        assertTrue(result);
        assertEquals(1, node.countSubscriptions(me));

        result = node.unsubscribe(me);
        assertTrue(result);
        assertEquals(0, node.countSubscriptions(me));
    }

    public void testUnsubscribeNonMatchingEntity() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");
        node.subscribe("id1", me);

        boolean result = node.unsubscribe("someotherid", me);
        assertFalse(result);
        assertTrue(node.isSubscribed(me));
        assertTrue(node.isSubscribed("id1"));
    }

    public void testSubscriptionCount() throws Exception {
        Entity me = EntityImpl.parse("me@vysper.org");
        node.subscribe("id1", me);
        node.subscribe("id2", me);
        Entity you = EntityImpl.parse("you@vysper.org");
        node.subscribe("id3", you);

        assertEquals(3, node.countSubscriptions());
    }
}
