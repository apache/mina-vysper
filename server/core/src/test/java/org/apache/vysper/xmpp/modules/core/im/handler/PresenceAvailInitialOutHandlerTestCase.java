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

import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.PROBE;

import java.util.List;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

/**
 */
public class PresenceAvailInitialOutHandlerTestCase extends PresenceHandlerBaseTestCase {

    protected PresenceHandler handler = new PresenceHandler();

    public void testInitialPresence() throws BindException, EntityFormatException {
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(
                StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), null, null, null, null, null).build());

        assertEquals(ResourceState.CONNECTED, getResourceState());
        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        // check resource state change, do not override interested
        assertEquals(ResourceState.AVAILABLE, getResourceState());

        // 3 intial presence broadcasts to same session (but not to non-available) + 2
        // presence to subscribers + 2 probes to subscriptions
        assertEquals(3 + 2 + 2, ((StanzaReceiverRelay) sessionContext.getStanzaRelay()).getCountDelivered());

        //
        // check presence broadcasts to resources of same session (self, interested &
        // available)
        //

        Stanza initiatorNotification = initiatingUser.getNextStanza();
        assertNotNull(initiatorNotification);
        assertTrue(checkPresence(initiatorNotification, initiatingUser.getEntityFQ(), null));
        assertTrue(initiatorNotification.getVerifier()
                .toAttributeEquals(initiatingUser.getEntityFQ().getFullQualifiedName()));

        Stanza availableResourceNotification = anotherAvailableUser.getNextStanza();
        assertNotNull(availableResourceNotification);
        assertTrue(checkPresence(availableResourceNotification, initiatingUser.getEntityFQ(), null));
        assertTrue(availableResourceNotification.getVerifier()
                .toAttributeEquals(anotherAvailableUser.getEntityFQ().getFullQualifiedName()));
        assertNull(anotherAvailableUser.getNextStanza()); // no more stanzas

        Stanza interestedResourceNotification = anotherInterestedUser.getNextStanza();
        assertNotNull(interestedResourceNotification);
        assertTrue(checkPresence(interestedResourceNotification, initiatingUser.getEntityFQ(), null));
        assertTrue(interestedResourceNotification.getVerifier().toAttributeEquals(
                initiatingUser.getEntity().getFullQualifiedName() + "/" + anotherInterestedUser.getBoundResourceId()));
        assertNull(anotherInterestedUser.getNextStanza()); // no more stanzas

        assertNull(anotherInterestedNotAvailUser.getNextStanza()); // no stanza at all

        //
        // check other presences
        //

        assertNull(unrelatedUser.getNextStanza()); // does not sent pres to everybody arbitrarily
        assertTrue(checkPresence(subscribed_FROM.getNextStanza(), initiatingUser.getEntityFQ(), null)); // pres sent to
                                                                                                        // FROM contacts
        assertNull(subscribed_FROM.getNextStanza()); // no second stanza sent to FROMs

        // initial pres and pres probe might come in different order
        assertTrue(checkPresence(subscribed_BOTH.getNextStanza(), initiatingUser.getEntityFQ(), null)); // pres sent to
                                                                                                        // BOTH contacts
        assertTrue(checkPresence(subscribed_BOTH.getNextStanza(), initiatingUser.getEntityFQ(), PROBE)); // probe sent
                                                                                                         // to BOTH
                                                                                                         // contacts
        assertNull(subscribed_BOTH.getNextStanza()); // no third stanza sent to BOTHs

        assertTrue(checkPresence(subscribed_TO.getNextStanza(), initiatingUser.getEntityFQ(), PROBE)); // probe sent to
                                                                                                       // TO contacts
        assertNull(subscribed_TO.getNextStanza()); // pres NOT sent to TO contacts
    }

    public void testInitialPresenceWithoutFrom() throws BindException, EntityFormatException, XMLSemanticError {
        // after setUp(), there is more than one bound resource
        // so, if leaving from == null, the handler will not know from which resource
        // the presence really comes...
        XMPPCoreStanza initialPresence = XMPPCoreStanza
                .getWrapper(StanzaBuilder.createPresenceStanza(null, null, null, null, null, null).build());

        List<Stanza> stanzas = handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true,
                sessionContext, new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        Stanza stanza = stanzas.get(0);
        // ... and will give an error:
        assertEquals("error", stanza.getAttribute("type").getValue());
        assertEquals(StanzaErrorCondition.UNKNOWN_SENDER.value(),
                stanza.getSingleInnerElementsNamed("error").getFirstInnerElement().getName());

        sessionContext.getServerRuntimeContext().getResourceRegistry()
                .unbindResource(anotherInterestedNotAvailUser.getBoundResourceId());
        sessionContext.getServerRuntimeContext().getResourceRegistry()
                .unbindResource(anotherInterestedUser.getBoundResourceId());
        sessionContext.getServerRuntimeContext().getResourceRegistry()
                .unbindResource(anotherAvailableUser.getBoundResourceId());
        // 3 other resources got unbound, remaining one should now be unique
        stanzas = handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        stanza = stanzas.get(0);
        assertNull(stanza); // no return, esp no error stanza - all the handling is done through relays
        stanza = initiatingUser.getNextStanza();
        assertNull(stanza.getAttribute("type"));
        assertEquals(0, stanza.getInnerElements().size());
        assertNull(initiatingUser.getNextStanza()); // no second stanza

        // now we run berserk :-) - when there is no resource remaining, from-resolution
        // fails again, and we get the same error again
        boolean noRemainingBinds = sessionContext.getServerRuntimeContext().getResourceRegistry()
                .unbindResource(initiatingUser.getBoundResourceId());
        assertTrue(noRemainingBinds);
        stanzas = handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        stanza = stanzas.get(0);
        assertEquals("error", stanza.getAttribute("type").getValue());
        assertEquals(StanzaErrorCondition.UNKNOWN_SENDER.value(),
                stanza.getSingleInnerElementsNamed("error").getFirstInnerElement().getName());
    }

}
