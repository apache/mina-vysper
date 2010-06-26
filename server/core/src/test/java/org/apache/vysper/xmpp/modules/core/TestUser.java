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
package org.apache.vysper.xmpp.modules.core;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

public class TestUser {
    protected String boundResourceId;

    protected Entity entity;

    protected StanzaReceiverQueue queue;

    protected Entity fqEntity;

    public static TestUser createForSession(TestSessionContext sessionContext, Entity entity) throws BindException {
        return createForSession(sessionContext, entity, true);
    }

    public static TestUser createForSession(TestSessionContext sessionContext, Entity entity, boolean receiveForFullJID)
            throws BindException {
        String boundResourceId = sessionContext.bindResource();
        StanzaReceiverQueue queue = sessionContext.addReceiver(entity, receiveForFullJID ? boundResourceId : null);
        return new TestUser(boundResourceId, entity, queue);
    }

    public static TestUser createQueueReceiver(TestSessionContext parentSession, String entity) throws BindException,
            EntityFormatException {
        return createQueueReceiver(parentSession, EntityImpl.parse(entity));
    }

    public static TestUser createQueueReceiver(TestSessionContext parentSession, Entity entity) throws BindException {
        StanzaReceiverQueue queue = parentSession.addReceiver(entity, null);
        return new TestUser(null, entity, queue);
    }

    public static TestUser createContact(TestSessionContext parentSession, MemoryRosterManager rosterManager,
            String entity, SubscriptionType subscriptionType) throws BindException, EntityFormatException,
            RosterException {
        TestUser testUser = createQueueReceiver(parentSession, EntityImpl.parse(entity));
        rosterManager.addContact(parentSession.getInitiatingEntity(), new RosterItem(testUser.getEntity(),
                subscriptionType));
        return testUser;
    }

    public TestUser(String boundResourceId, Entity entity, StanzaReceiverQueue queue) {
        this.boundResourceId = boundResourceId;
        this.entity = entity;
        this.queue = queue;
        this.fqEntity = new EntityImpl(entity, boundResourceId);
    }

    public String getBoundResourceId() {
        return boundResourceId;
    }

    public Entity getEntity() {
        return entity;
    }

    public StanzaReceiverQueue getQueue() {
        return queue;
    }

    public Stanza getNextStanza() {
        return queue.getNext();
    }

    public Entity getEntityFQ() {
        return fqEntity;
    }
}
