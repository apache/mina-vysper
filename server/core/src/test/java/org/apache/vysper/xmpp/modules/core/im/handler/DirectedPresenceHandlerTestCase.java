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

import java.util.Set;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

/**
 */
public class DirectedPresenceHandlerTestCase extends PresenceHandlerBaseTestCase {

    protected PresenceHandler handler = new PresenceHandler();

    public void testUpdatedPresence() throws BindException, EntityFormatException {
        StanzaReceiverRelay receiverRelay = (StanzaReceiverRelay) sessionContext.getStanzaRelay();
        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        // at first, initial presence
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder
                .createPresenceStanza(initiatingUser.getEntityFQ(), unrelatedUser.getEntityFQ(), null, null, null, null)
                .build());
        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));
        assertTrue(0 < receiverRelay.getCountDelivered());

        // directed presence has been recorded internally
        Set<Entity> map = (Set<Entity>) sessionContext
                .getAttribute("DIRECTED_PRESENCE_MAP_" + initiatingUser.getBoundResourceId());
        assertNotNull(map);
        assertEquals(map.iterator().next(), unrelatedUser.getEntityFQ());

        Stanza directedPresence = unrelatedUser.getNextStanza();
        assertNotNull(directedPresence);
        assertTrue(PresenceStanza.isOfType(directedPresence)); // is presence
        PresenceStanza presenceStanza = new PresenceStanza(directedPresence);
        assertEquals(initiatingUser.getEntityFQ(), presenceStanza.getFrom());

        resetRecordedStanzas(); // purge recorded

        // directed unavailable presence
        XMPPCoreStanza directedUnvailPresence = XMPPCoreStanza
                .getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(),
                        unrelatedUser.getEntityFQ(), null, PresenceStanzaType.UNAVAILABLE, null, null).build());
        handler.executeCore(directedUnvailPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));
        assertTrue(0 < receiverRelay.getCountDelivered());

        // directed presence has been recorded internally
        map = (Set<Entity>) sessionContext.getAttribute("DIRECTED_PRESENCE_MAP_" + initiatingUser.getBoundResourceId());
        assertTrue(map.size() == 0);
        ResourceState resourceState = sessionContext.getServerRuntimeContext().getResourceRegistry()
                .getResourceState(initiatingUser.getBoundResourceId());
        assertTrue(ResourceState.isAvailable(resourceState));

        Stanza directedUnavailPresence = unrelatedUser.getNextStanza();
        assertNotNull(directedUnavailPresence);
        assertTrue(PresenceStanza.isOfType(directedUnavailPresence)); // is presence
        presenceStanza = new PresenceStanza(directedUnavailPresence);
        assertEquals(initiatingUser.getEntityFQ(), presenceStanza.getFrom());
        assertEquals(PresenceStanzaType.UNAVAILABLE, presenceStanza.getPresenceType());

    }

    public void testUnavailableForDirectedPresences() throws BindException, EntityFormatException {
        StanzaReceiverRelay receiverRelay = (StanzaReceiverRelay) sessionContext.getStanzaRelay();
        setResourceState(initiatingUser.getBoundResourceId(), ResourceState.AVAILABLE_INTERESTED);

        // at first, initial presence
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder
                .createPresenceStanza(initiatingUser.getEntityFQ(), unrelatedUser.getEntityFQ(), null, null, null, null)
                .build());
        handler.executeCore(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));

        // directed presence has been recorded internally
        Set<Entity> map = (Set<Entity>) sessionContext
                .getAttribute("DIRECTED_PRESENCE_MAP_" + initiatingUser.getBoundResourceId());
        assertEquals(map.iterator().next(), unrelatedUser.getEntityFQ());

        resetRecordedStanzas(); // purge recorded

        // GENERAL unavailable presence
        XMPPCoreStanza generalUnavailable = XMPPCoreStanza
                .getWrapper(StanzaBuilder.createPresenceStanza(initiatingUser.getEntityFQ(), null, null,
                        PresenceStanzaType.UNAVAILABLE, null, null).build());
        handler.executeCore(generalUnavailable, sessionContext.getServerRuntimeContext(), true, sessionContext,
                new DefaultStanzaBroker(receiverRelay, sessionContext));
        assertTrue(0 < receiverRelay.getCountDelivered());
        ResourceState resourceState = sessionContext.getServerRuntimeContext().getResourceRegistry()
                .getResourceState(initiatingUser.getBoundResourceId());
        assertFalse(ResourceState.isAvailable(resourceState));

        // directed presence has been recorded internally
        map = (Set<Entity>) sessionContext.getAttribute("DIRECTED_PRESENCE_MAP_" + initiatingUser.getBoundResourceId());
        assertEquals(0, map.size());

        Stanza directedUnavailPresence = unrelatedUser.getNextStanza();
        assertNotNull(directedUnavailPresence);
        assertTrue(PresenceStanza.isOfType(directedUnavailPresence)); // is presence
        PresenceStanza presenceStanza = new PresenceStanza(directedUnavailPresence);
        assertEquals(initiatingUser.getEntityFQ(), presenceStanza.getFrom());
        assertEquals(PresenceStanzaType.UNAVAILABLE, presenceStanza.getPresenceType());

    }

}