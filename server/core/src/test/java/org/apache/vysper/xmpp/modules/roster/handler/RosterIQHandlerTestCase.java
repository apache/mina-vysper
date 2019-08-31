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
package org.apache.vysper.xmpp.modules.roster.handler;

import static org.mockito.Mockito.mock;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;

import junit.framework.TestCase;

/**
 */
public class RosterIQHandlerTestCase extends TestCase {

    private TestSessionContext sessionContext;

    private String boundResourceId;

    protected MemoryRosterManager rosterManager;

    protected EntityImpl client;

    protected RosterIQHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        client = EntityImpl.parse("tester@vysper.org");
        sessionContext.setInitiatingEntity(client);
        boundResourceId = sessionContext.bindResource();
        sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(boundResourceId,
                ResourceState.CONNECTED);
        rosterManager = new MemoryRosterManager();
        OpenStorageProviderRegistry storageProviderRegistry = new OpenStorageProviderRegistry();
        storageProviderRegistry.add(rosterManager);
        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext())
                .setStorageProviderRegistry(storageProviderRegistry);
        handler = new RosterIQHandler();
    }

    public void testRosterGet() {

        StanzaBuilder stanzaBuilder = createRosterGet();

        assertEquals(ResourceState.CONNECTED, getResourceState());
        handler.execute(stanzaBuilder.build(), sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                mock(StanzaBroker.class));
        assertEquals(ResourceState.CONNECTED_INTERESTED, getResourceState());

        // C: <iq from='juliet@example.com/balcony'
        // type='get'
        // id='roster_get'>
        // <query xmlns='jabber:iq:roster'/>
        // </iq>

    }

    public void testRosterState_WithRosterGetAfterInitialPresence() {

        StanzaBuilder stanzaBuilder = createRosterGet();

        // mock intial presence resource state change
        sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(boundResourceId,
                ResourceState.AVAILABLE);

        handler.execute(stanzaBuilder.build(), sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                mock(StanzaBroker.class));
        assertEquals(ResourceState.AVAILABLE_INTERESTED, getResourceState());
    }

    private StanzaBuilder createRosterGet() {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(new EntityImpl(client, boundResourceId), null,
                IQStanzaType.GET, "id1");
        stanzaBuilder.startInnerElement("query", NamespaceURIs.JABBER_IQ_ROSTER).endInnerElement();
        return stanzaBuilder;
    }

    private ResourceState getResourceState() {
        return sessionContext.getServerRuntimeContext().getResourceRegistry().getResourceState(boundResourceId);
    }

}
