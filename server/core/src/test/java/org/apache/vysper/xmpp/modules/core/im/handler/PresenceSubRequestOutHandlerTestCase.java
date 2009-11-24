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

import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.*;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.*;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

/**
 */
public class PresenceSubRequestOutHandlerTestCase extends PresenceHandlerBaseTestCase {

    protected PresenceHandler handler = new PresenceHandler();

    public void testUnrelatedApprovesSubscription_Inbound() throws BindException, EntityFormatException {

        requestSubscribeToUnrelated_Outbound();
        assertStanzasDeliveredAndRelayed(0);

        // now entity 'unrelated' approves the subscription

        XMPPCoreStanza requestApproval = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(unrelatedUser.getEntityFQ(), initiatingUser.getEntity(), null, PresenceStanzaType.SUBSCRIBED, null, null).build());
        handler.executeCore(requestApproval, sessionContext.getServerRuntimeContext(), false, sessionContext);

        // 3 roster pushes but...
        for (int i = 1; i <= 3; i++) {
            Stanza stanza = sessionContext.getNextRecordedResponse();
            assertEquals("iq", stanza.getName());
        }
        // ... BUT no subscription approval (presence) or anything additional to the roster pushes
        assertNull(sessionContext.getNextRecordedResponse());

        resetRecordedStanzas();
    }

    private void requestSubscribeToUnrelated_Outbound() {
        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        // SUBSCRIBE FROM
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), unrelatedUser.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).build());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        // 1 to TO + 3 roster pushes
        assertStanzasDeliveredAndRelayed(1);
        assertStanzasReceivedDirectly(3);

        // roster push for 1 interested initiator of _same_ session
        Stanza initiatorNotification = getNextDirectResponseFor(initiatingUser);
        assertTrue(checkRosterPush(initiatorNotification, initiatingUser.getEntityFQ(), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));

        // no stanzas for not interested
        assertNull(getNextDirectResponseFor(anotherAvailableUser));

        // roster 2 interested resources of _same_ session...
        
        // roster push for interested
        Stanza interestedResourceNotification = getNextDirectResponseFor(anotherInterestedUser);
        assertTrue(checkRosterPush(interestedResourceNotification, new EntityImpl(initiatingUser.getEntity(), anotherInterestedUser.getBoundResourceId()), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));
        assertNull(getNextDirectResponseFor(anotherInterestedUser)); // no more stanzas;

        // roster push for interested but not avail
        Stanza interestedNotYetAvailResourceNotification = getNextDirectResponseFor(anotherInterestedNotAvailUser);
        assertTrue(checkRosterPush(interestedNotYetAvailResourceNotification, new EntityImpl(initiatingUser.getEntity(), anotherInterestedNotAvailUser.getBoundResourceId()), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));
        assertNull(getNextDirectResponseFor(anotherInterestedNotAvailUser)); // no more stanzas;

        // sub request sent to contact
        assertTrue(checkPresence(unrelatedUser.getNextStanza(), PresenceStanzaType.SUBSCRIBE, initiatingUser.getEntity(), null));
        assertNull(unrelatedUser.getNextStanza()); // no other stanza

        assertTrue(checkRosterItem(unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));

        resetRecordedStanzas();
    }

    public void testWithFROM() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        // SUBSCRIBE FROM
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_FROM.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).build());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        // 1 to TO + 3 roster pushes
        assertStanzasDeliveredAndRelayed(1);
        assertStanzasReceivedDirectly(3);

        // roster push for 1 interested initiator...
        Stanza initiatorNotification = getNextDirectResponseFor(initiatingUser);
        assertTrue(checkRosterPush(initiatorNotification, initiatingUser.getEntityFQ(), subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));

        // .. and 2 interested resources of _same_ session
        Stanza anotherInterestedUserNotification = getNextDirectResponseFor(anotherInterestedUser);
        assertTrue(checkRosterPush(anotherInterestedUserNotification, anotherInterestedUser.getEntityFQ(), subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));
        Stanza anotherInterestedNotAvailUserNotification = getNextDirectResponseFor(anotherInterestedNotAvailUser);
        assertTrue(checkRosterPush(anotherInterestedNotAvailUserNotification, anotherInterestedNotAvailUser.getEntityFQ(), subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));

        assertNull(sessionContext.getNextRecordedResponse()); // no more stanzas

        // sub request sent to contact
        assertTrue(checkPresence(subscribed_FROM.getNextStanza(), PresenceStanzaType.SUBSCRIBE, initiatingUser.getEntity(), null)); 
        assertNull(subscribed_FROM.getNextStanza()); // no other stanza

        assertTrue(checkRosterItem(subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));
    }

    public void testWithTO() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_TO.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).build());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        assertStanzasDeliveredAndRelayed(0); // TO subscription already exists
    }

    public void testWithBOTH() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_BOTH.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).build());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        assertStanzasDeliveredAndRelayed(0); // TO subscription already exists with subscribed_BOTH
    }

}
