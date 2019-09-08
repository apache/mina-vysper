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
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

/**
 */
public class PresenceAvailUpdateOutHandlerTestCase extends PresenceHandlerBaseTestCase {

    protected PresenceHandler handler = new PresenceHandler();

    public void testUpdatedPresence() throws BindException, EntityFormatException {
        StanzaReceiverRelay receiverRelay = (StanzaReceiverRelay) sessionContext.getStanzaRelay();

        // at first, initial presence
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(
                StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), null, null, null, null, null).build());
        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));
        assertTrue(0 < receiverRelay.getCountDelivered());
        resetRecordedStanzas(); // purge recorded
        assertTrue(0 == receiverRelay.getCountDelivered());

        // send update now
        final String showValue = "chatty";

        XMPPCoreStanza updatePresence = XMPPCoreStanza.getWrapper(StanzaBuilder
                .createPresenceStanza(initiatingUser.getEntityFQ(), null, null, null, showValue, null).build());
        handler.executeCore(updatePresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));
        // check resource state
        assertEquals(ResourceState.AVAILABLE, getResourceState());

        // 3 presence update broadcasts to same session + 2 presence to subscribers
        assertEquals(3 + 2, ((StanzaReceiverRelay) sessionContext.getStanzaRelay()).getCountDelivered());

        //
        // check presence broadcasts to resources of same session (self, interested &
        // available)
        //

        Stanza initiatorNotification = initiatingUser.getNextStanza();
        assertNotNull(initiatorNotification);
        assertTrue(checkPresence(initiatorNotification, null, initiatingUser.getEntityFQ(), showValue));
        assertTrue(initiatorNotification.getVerifier()
                .toAttributeEquals(initiatingUser.getEntityFQ().getFullQualifiedName()));

        Stanza availableResourceNotification = anotherAvailableUser.getNextStanza();
        assertNotNull(availableResourceNotification);
        assertTrue(checkPresence(availableResourceNotification, null, initiatingUser.getEntityFQ(), showValue));
        assertTrue(availableResourceNotification.getVerifier()
                .toAttributeEquals(anotherAvailableUser.getEntityFQ().getFullQualifiedName()));
        assertNull(anotherAvailableUser.getNextStanza()); // no more stanzas

        Stanza interestedResourceNotification = anotherInterestedUser.getNextStanza();
        assertNotNull(interestedResourceNotification);
        assertTrue(checkPresence(interestedResourceNotification, null, initiatingUser.getEntityFQ(), showValue));
        assertTrue(interestedResourceNotification.getVerifier().toAttributeEquals(
                initiatingUser.getEntity().getFullQualifiedName() + "/" + anotherInterestedUser.getBoundResourceId()));
        assertNull(anotherInterestedUser.getNextStanza()); // no more stanzas

        assertNull(anotherInterestedNotAvailUser.getNextStanza()); // no stanza at all

        //
        // check other presences
        //

        assertNull(unrelatedUser.getNextStanza()); // does not sent pres to everybody arbitrarily
        assertTrue(checkPresence(subscribed_FROM.getNextStanza(), null, initiatingUser.getEntityFQ(), showValue)); // pres
                                                                                                                   // sent
                                                                                                                   // to
                                                                                                                   // FROM
                                                                                                                   // contacts
        assertNull(subscribed_FROM.getNextStanza()); // no second stanza sent to FROMs

        // initial pres and pres probe might come in different order
        assertTrue(checkPresence(subscribed_BOTH.getNextStanza(), null, initiatingUser.getEntityFQ(), showValue)); // pres
                                                                                                                   // sent
                                                                                                                   // to
                                                                                                                   // BOTH
                                                                                                                   // contacts
        assertNull(subscribed_BOTH.getNextStanza()); // no second stanza (especially probe) sent to BOTHs

        assertNull(subscribed_TO.getNextStanza()); // pres (especially probe) NOT sent to TO contacts
    }

}
