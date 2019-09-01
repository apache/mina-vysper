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
package org.apache.vysper.xmpp.modules.core.im.handler;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.core.TestUser;
import org.apache.vysper.xmpp.modules.roster.AskSubscriptionType;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterUtils;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

import junit.framework.TestCase;

/**
 * base class for subclassing presence handler tests from
 */
abstract public class PresenceHandlerBaseTestCase extends TestCase {

    protected TestSessionContext sessionContext;

    protected MemoryRosterManager rosterManager;

    protected TestUser initiatingUser;

    protected TestUser anotherInterestedNotAvailUser;

    protected TestUser anotherInterestedUser;

    protected TestUser anotherAvailableUser;

    protected TestUser unrelatedUser;

    protected TestUser subscribed_TO;

    protected TestUser subscribed_BOTH;

    protected TestUser subscribed_FROM;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        rosterManager = new MemoryRosterManager();
        OpenStorageProviderRegistry storageProviderRegistry = new OpenStorageProviderRegistry();
        storageProviderRegistry.add(rosterManager);
        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext())
                .setStorageProviderRegistry(storageProviderRegistry);

        Entity client = EntityImpl.parse("tester@vysper.org");
        sessionContext.setInitiatingEntity(client);
        initiatingUser = TestUser.createForSession(sessionContext, client);

        // set up more resources for the same session
        anotherInterestedNotAvailUser = TestUser.createForSession(sessionContext, client);
        setResourceState(anotherInterestedNotAvailUser.getBoundResourceId(), ResourceState.CONNECTED_INTERESTED);

        anotherInterestedUser = TestUser.createForSession(sessionContext, client);
        setResourceState(anotherInterestedUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        anotherAvailableUser = TestUser.createForSession(sessionContext, client);
        setResourceState(anotherAvailableUser.getBoundResourceId(), ResourceState.AVAILABLE);

        // set up completely unrelated resource
        unrelatedUser = TestUser.createQueueReceiver(sessionContext, "unrelated@vysper.org");

        // now we have:
        // 4 resources for the same entity: one initiating, one interested (not yet
        // avail), one interested (implicitly avail), one available
        // and another unrelated resource

        subscribed_TO = TestUser.createContact(sessionContext, rosterManager, "subscribed_to@vysper.org",
                SubscriptionType.TO);
        subscribed_BOTH = TestUser.createContact(sessionContext, rosterManager, "subscribed_both@vysper.org",
                SubscriptionType.BOTH);
        subscribed_FROM = TestUser.createContact(sessionContext, rosterManager, "subscribed_from@vysper.org",
                SubscriptionType.FROM);
    }

    protected void setResourceState(String resourceId, ResourceState state) {
        sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(resourceId, state);
    }

    protected boolean checkPresence(Stanza stanza, Entity from, PresenceStanzaType presenceType) {
        return checkPresence(stanza, presenceType, from, null);
    }

    protected boolean checkPresence(Stanza stanza, PresenceStanzaType presenceType, Entity from, String show) {
        if (stanza == null)
            return false;
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        if (from == null) {
            if (stanza.getFrom() != null)
                return false;
        } else {
            if (!stanza.getFrom().equals(from))
                return false;
        }
        if (!xmlElementVerifier.nameEquals("presence"))
            return false;
        if (presenceType == null && xmlElementVerifier.attributePresent("type"))
            return false;
        if (presenceType != null && !xmlElementVerifier.attributeEquals("type", presenceType.value()))
            return false;
        try {
            if (show != null && !xmlElementVerifier.subElementPresent("show")
                    && !stanza.getSingleInnerElementsNamed("show").getSingleInnerText().getText().equals(show)) {
                return false;
            }
        } catch (XMLSemanticError xmlSemanticError) {
            return false;
        }
        return true;
    }

    protected ResourceState getResourceState() {
        return sessionContext.getServerRuntimeContext().getResourceRegistry()
                .getResourceState(initiatingUser.getBoundResourceId());
    }

    protected void assertStanzasDeliveredAndRelayed(int expectedRelayedAndDelivered) {
        assertStanzasRelayed(expectedRelayedAndDelivered, expectedRelayedAndDelivered);
    }

    protected void assertStanzasRelayed(int expectedRelayed, int expectedDelivered) {
        assertEquals(expectedRelayed, ((StanzaReceiverRelay) sessionContext.getStanzaRelay()).getCountRelayed());
        assertEquals(expectedDelivered, ((StanzaReceiverRelay) sessionContext.getStanzaRelay()).getCountDelivered());
    }

    protected boolean checkRosterItem(Entity contactEntity, SubscriptionType expectedSubscriptionType,
            AskSubscriptionType expectedAskSubscriptionType) {
        try {
            RosterItem contact = rosterManager.getContact(initiatingUser.getEntity(), contactEntity);
            assertNotNull(contact);
            assertEquals(expectedSubscriptionType, contact.getSubscriptionType());
            assertEquals(expectedAskSubscriptionType, contact.getAskSubscriptionType());
        } catch (RosterException e) {
            fail(e.toString());
        }
        return true;
    }

    protected boolean checkRosterPush(Stanza stanza, Entity entity, Entity contact, SubscriptionType subscriptionType,
            AskSubscriptionType askSubscriptionType) {
        if (stanza == null)
            return false;
        IQStanza rosterPush = (IQStanza) XMPPCoreStanza.getWrapper(stanza);
        rosterPush.getInnerElementsNamed("query");

        RosterItem rosterItem = null;
        try {
            rosterItem = RosterUtils.parseRosterItemForTesting(rosterPush);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals(rosterPush.getTo().getFullQualifiedName(), entity.getFullQualifiedName());
        assertEquals(contact.getFullQualifiedName(), rosterItem.getJid().getFullQualifiedName());
        assertEquals(subscriptionType, rosterItem.getSubscriptionType());
        assertEquals(subscriptionType, rosterItem.getSubscriptionType());
        assertEquals(askSubscriptionType, rosterItem.getAskSubscriptionType());

        return true;
    }

    protected void resetRecordedStanzas() {
        StanzaReceiverRelay receiverRelay = (StanzaReceiverRelay) sessionContext.getStanzaRelay();
        receiverRelay.resetAll();
    }

    /**
     * searches for the next message captured by the test session which was sent
     * directly to the given user's resource
     * 
     * @param testUser
     * @return NULL, if no stanza available matching the user's resource id
     */
    protected Stanza getNextRelayedResponseFor(TestUser testUser) {
        return testUser.getNextStanza();
    }
}
