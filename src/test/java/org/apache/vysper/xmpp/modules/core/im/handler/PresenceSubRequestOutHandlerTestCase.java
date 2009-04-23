/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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

        XMPPCoreStanza requestApproval = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(unrelatedUser.getEntityFQ(), initiatingUser.getEntity(), null, PresenceStanzaType.SUBSCRIBED, null, null).getFinalStanza());
        handler.executeCore(requestApproval, sessionContext.getServerRuntimeContext(), false, sessionContext);

        // subscribed stanza not delivered to client
        assertNull(sessionContext.getNextRecordedResponse());
        // 3 roster pushes
        assertStanzasDeliveredAndRelayed(3);

        resetRecordedStanzas();
    }

    private void requestSubscribeToUnrelated_Outbound() {
        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), unrelatedUser.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).getFinalStanza());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        // 1 to TO + 3 roster pushes
        assertStanzasDeliveredAndRelayed(1+3);

        // roster push for interested initiator
        Stanza initiatorNotification = initiatingUser.getNextStanza();
        assertTrue(checkRosterPush(initiatorNotification, initiatingUser.getEntityFQ(), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));
        assertNull(initiatingUser.getNextStanza()); // no more stanzas

        // no stanzas for not interested
        assertNull(anotherAvailableUser.getNextStanza());

        // roster push for interested
        Stanza interestedResourceNotification = anotherInterestedUser.getNextStanza();
        assertTrue(checkRosterPush(interestedResourceNotification, new EntityImpl(initiatingUser.getEntity(), anotherInterestedUser.getBoundResourceId()), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));
        assertNull(anotherInterestedUser.getNextStanza()); // no more stanzas

        // roster push for interested
        Stanza interestedNotYetAvailResourceNotification = anotherInterestedNotAvailUser.getNextStanza();
        assertTrue(checkRosterPush(interestedNotYetAvailResourceNotification, new EntityImpl(initiatingUser.getEntity(), anotherInterestedNotAvailUser.getBoundResourceId()), unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));
        assertNull(anotherInterestedNotAvailUser.getNextStanza()); // no more stanzas

        // sub request sent to contact
        assertTrue(checkPresence(unrelatedUser.getNextStanza(), PresenceStanzaType.SUBSCRIBE, initiatingUser.getEntity(), null));
        assertNull(unrelatedUser.getNextStanza()); // no other stanza

        assertTrue(checkRosterItem(unrelatedUser.getEntity(), NONE, ASK_SUBSCRIBE));

        resetRecordedStanzas();
    }

    public void testWithFROM() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_FROM.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).getFinalStanza());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        // 1 to TO + 3 roster pushes
        assertStanzasDeliveredAndRelayed(1+3);

        // roster push for interested initiator
        Stanza initiatorNotification = initiatingUser.getNextStanza();
        assertTrue(checkRosterPush(initiatorNotification, initiatingUser.getEntityFQ(), subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));
        assertNull(initiatingUser.getNextStanza()); // no more stanzas

        // sub request sent to contact
        assertTrue(checkPresence(subscribed_FROM.getNextStanza(), PresenceStanzaType.SUBSCRIBE, initiatingUser.getEntity(), null)); 
        assertNull(subscribed_FROM.getNextStanza()); // no other stanza

        assertTrue(checkRosterItem(subscribed_FROM.getEntity(), FROM, ASK_SUBSCRIBE));
    }

    public void testWithTO() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_TO.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).getFinalStanza());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        assertStanzasDeliveredAndRelayed(0); // TO subscription already exists
    }

    public void testWithBOTH() throws BindException, EntityFormatException {

        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), subscribed_BOTH.getEntity(), null, PresenceStanzaType.SUBSCRIBE, null, null).getFinalStanza());

        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext);
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());

        assertStanzasDeliveredAndRelayed(0); // TO subscription already exists with subscribed_BOTH
    }

}
