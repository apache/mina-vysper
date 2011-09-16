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
package org.apache.vysper.xmpp.delivery.inbound;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.DefaultResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 */
public class DeliveringInteralInboundStanzaRelayTestCase extends TestCase {

    protected ResourceRegistry resourceRegistry = new DefaultResourceRegistry();

    protected AccountManagement accountVerification;

    protected DeliveringInternalInboundStanzaRelay stanzaRelay;

    static class AccountVerificationMock implements AccountManagement {
        public void addUser(Entity username, String password) throws AccountCreationException {
            ; // empty
        }

        public boolean verifyAccountExists(Entity jid) {
            return true;
        }

        public void changePassword(Entity username, String password) throws AccountCreationException {
            ; // empty
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        accountVerification = new AccountVerificationMock();
        stanzaRelay = new DeliveringInternalInboundStanzaRelay(EntityImpl.parse("vysper.org"), resourceRegistry,
                accountVerification, null);
    }

    public void testSimpleRelay() throws EntityFormatException, XMLSemanticError, DeliveryException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(null, null);
        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);

        EntityImpl fromEntity = EntityImpl.parse("userFrom@vysper.org");
        EntityImpl toEntity = EntityImpl.parse("userTo@vysper.org");
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(toEntity);
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        resourceRegistry.bindSession(sessionContext);

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").build();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            Stanza recordedStanza = sessionContext.getNextRecordedResponse(1000);
            assertNotNull("stanza delivered", recordedStanza);
            assertEquals("Hello", recordedStanza.getSingleInnerElementsNamed("body").getSingleInnerText().getText());
        } catch (DeliveryException e) {
            throw e;
        }
    }

    public void testSimpleRelayToUnboundSession() throws EntityFormatException, XMLSemanticError, DeliveryException {
        EntityImpl fromEntity = EntityImpl.parse("userFrom@vysper.org");
        EntityImpl toEntity = EntityImpl.parse("userTo@vysper.org");
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(toEntity);
        String resource = resourceRegistry.bindSession(sessionContext);
        boolean noResourceRemains = resourceRegistry.unbindResource(resource);
        assertTrue(noResourceRemains);

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").build();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            Stanza recordedStanza = sessionContext.getNextRecordedResponse(1000);
            assertNull("stanza not delivered to unbound", recordedStanza);
        } catch (DeliveryException e) {
            throw e;
        }
    }

    public void testRelayToTwoRecepients_DeliverToALL() throws EntityFormatException, XMLSemanticError,
            DeliveryException, BindException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(null, null);

        // !! DeliverMessageToHighestPriorityResourcesOnly = FALSE
        serverRuntimeContext.getServerFeatures().setDeliverMessageToHighestPriorityResourcesOnly(false);

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);

        EntityImpl fromEntity = EntityImpl.parse("userFrom@vysper.org");

        EntityImpl toEntity = EntityImpl.parse("userTo@vysper.org");

        TestSessionContext sessionContextToEntity_1_prio3 = createSessionForTo(toEntity, 3); // NON-NEGATIVE
        TestSessionContext sessionContextToEntity_2_prio0 = createSessionForTo(toEntity, 0); // NON-NEGATIVE
        TestSessionContext sessionContextToEntity_3_prio3 = createSessionForTo(toEntity, 3); // NON-NEGATIVE
        TestSessionContext sessionContextToEntity_4_prioMinus = createSessionForTo(toEntity, -1); // not receiving, negative

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").build();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            Stanza recordedStanza_1 = sessionContextToEntity_1_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 1 delivered", recordedStanza_1);
            Stanza recordedStanza_2 = sessionContextToEntity_2_prio0.getNextRecordedResponse(100);
            assertNotNull("stanza 2 delivered", recordedStanza_2);
            Stanza recordedStanza_3 = sessionContextToEntity_3_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 3 delivered", recordedStanza_3);
            Stanza recordedStanza_4 = sessionContextToEntity_4_prioMinus.getNextRecordedResponse(100);
            assertNull("stanza 4 delivered", recordedStanza_4);
        } catch (DeliveryException e) {
            throw e;
        }

    }

    public void testRelayToTwoRecepients_DeliverToHIGHEST() throws EntityFormatException, XMLSemanticError,
            DeliveryException, BindException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(null, null);

        // !! DeliverMessageToHighestPriorityResourcesOnly = TRUE
        serverRuntimeContext.getServerFeatures().setDeliverMessageToHighestPriorityResourcesOnly(true);

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);

        EntityImpl fromEntity = EntityImpl.parse("userFrom@vysper.org");

        EntityImpl toEntity = EntityImpl.parse("userTo@vysper.org");

        TestSessionContext sessionContextToEntity_1_prio3 = createSessionForTo(toEntity, 3); // HIGHEST PRIO
        TestSessionContext sessionContextToEntity_2_prio0 = createSessionForTo(toEntity, 1); // not receiving
        TestSessionContext sessionContextToEntity_3_prio3 = createSessionForTo(toEntity, 3); // HIGHEST PRIO
        TestSessionContext sessionContextToEntity_4_prioMinus = createSessionForTo(toEntity, -1); // not receiving

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").build();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            Stanza recordedStanza_1 = sessionContextToEntity_1_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 1 delivered", recordedStanza_1);
            Stanza recordedStanza_2 = sessionContextToEntity_2_prio0.getNextRecordedResponse(100);
            assertNull("stanza 2 not delivered", recordedStanza_2);
            Stanza recordedStanza_3 = sessionContextToEntity_3_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 3 delivered", recordedStanza_3);
            Stanza recordedStanza_4 = sessionContextToEntity_4_prioMinus.getNextRecordedResponse(100);
            assertNull("stanza 4 not delivered", recordedStanza_4);
        } catch (DeliveryException e) {
            throw e;
        }

    }

    private TestSessionContext createSessionForTo(EntityImpl toEntity, final int priority) {
        TestSessionContext sessionContextToEntity = TestSessionContext.createSessionContext(toEntity);
        sessionContextToEntity.setSessionState(SessionState.AUTHENTICATED);
        String toEntityRes = resourceRegistry.bindSession(sessionContextToEntity);
        resourceRegistry.setResourcePriority(toEntityRes, priority);
        return sessionContextToEntity;
    }

}
