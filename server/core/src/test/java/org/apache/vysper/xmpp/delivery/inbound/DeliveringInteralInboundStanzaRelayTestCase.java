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

import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.delivery.failure.ServiceNotAvailableException;
import org.apache.vysper.xmpp.protocol.SimpleStanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.server.components.SimpleComponentRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.state.resourcebinding.DefaultResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.InternalResourceRegistry;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 */
public class DeliveringInteralInboundStanzaRelayTestCase extends TestCase {

    protected static final EntityImpl TO_ENTITY = EntityImpl.parseUnchecked("userTo@vysper.org");

    protected static final EntityImpl FROM_ENTITY = EntityImpl.parseUnchecked("userFrom@vysper.org");

    protected InternalResourceRegistry resourceRegistry = new DefaultResourceRegistry();

    protected AccountManagement accountVerification;

    protected DeliveringInternalInboundStanzaRelay stanzaRelay;
    
    private InternalSessionContext sessionContext = mock(InternalSessionContext.class);

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

    private Entity serverEntity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        accountVerification = new AccountVerificationMock();
        serverEntity = EntityImpl.parse("vysper.org");
        stanzaRelay = new DeliveringInternalInboundStanzaRelay(serverEntity, resourceRegistry,
                new SimpleComponentRegistry(serverEntity), accountVerification);
    }

    public void testSimpleRelay() throws EntityFormatException, XMLSemanticError, DeliveryException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity,
                mock(StanzaRelay.class));
        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        stanzaRelay.setStanzaHandlerExecutorFactory(new SimpleStanzaHandlerExecutorFactory(stanzaRelay));

        TestSessionContext sessionContext = TestSessionContext.createSessionContext(TO_ENTITY);
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        resourceRegistry.bindSession(sessionContext);

        Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "Hello").build();

        try {
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, IgnoreFailureStrategy.INSTANCE);
            Stanza recordedStanza = sessionContext.getNextRecordedResponse(1000);
            assertNotNull("stanza delivered", recordedStanza);
            assertEquals("Hello", recordedStanza.getSingleInnerElementsNamed("body").getSingleInnerText().getText());
        } catch (DeliveryException e) {
            throw e;
        }
    }

    public void testSimpleRelayToUnboundSession() throws EntityFormatException, XMLSemanticError, DeliveryException {
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(TO_ENTITY);
        String resource = resourceRegistry.bindSession(sessionContext);
        boolean noResourceRemains = resourceRegistry.unbindResource(resource);
        assertTrue(noResourceRemains);

        Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "Hello").build();

        try {
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, IgnoreFailureStrategy.INSTANCE);
            Stanza recordedStanza = sessionContext.getNextRecordedResponse(1000);
            assertNull("stanza not delivered to unbound", recordedStanza);
        } catch (DeliveryException e) {
            throw e;
        }
    }

    public void testRelayToTwoRecepients_DeliverToALL()
            throws EntityFormatException, XMLSemanticError, DeliveryException, BindException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity,
                mock(StanzaRelay.class));

        // !! DeliverMessageToHighestPriorityResourcesOnly = FALSE
        serverRuntimeContext.getServerFeatures().setDeliverMessageToHighestPriorityResourcesOnly(false);

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        stanzaRelay.setStanzaHandlerExecutorFactory(new SimpleStanzaHandlerExecutorFactory(stanzaRelay));

        TestSessionContext sessionContextTO_ENTITY_1_prio3 = createSessionForTo(TO_ENTITY, 3); // NON-NEGATIVE
        TestSessionContext sessionContextTO_ENTITY_2_prio0 = createSessionForTo(TO_ENTITY, 0); // NON-NEGATIVE
        TestSessionContext sessionContextTO_ENTITY_3_prio3 = createSessionForTo(TO_ENTITY, 3); // NON-NEGATIVE
        TestSessionContext sessionContextTO_ENTITY_4_prioMinus = createSessionForTo(TO_ENTITY, -1); // not receiving,
                                                                                                    // negative

        Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "Hello").build();

        try {
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, IgnoreFailureStrategy.INSTANCE);
            Stanza recordedStanza_1 = sessionContextTO_ENTITY_1_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 1 delivered", recordedStanza_1);
            Stanza recordedStanza_2 = sessionContextTO_ENTITY_2_prio0.getNextRecordedResponse(100);
            assertNotNull("stanza 2 delivered", recordedStanza_2);
            Stanza recordedStanza_3 = sessionContextTO_ENTITY_3_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 3 delivered", recordedStanza_3);
            Stanza recordedStanza_4 = sessionContextTO_ENTITY_4_prioMinus.getNextRecordedResponse(100);
            assertNull("stanza 4 delivered", recordedStanza_4);
        } catch (DeliveryException e) {
            throw e;
        }

    }

    public void testRelayToTwoRecepients_DeliverToHIGHEST()
            throws EntityFormatException, XMLSemanticError, DeliveryException, BindException {
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity,
                mock(StanzaRelay.class));

        // !! DeliverMessageToHighestPriorityResourcesOnly = TRUE
        serverRuntimeContext.getServerFeatures().setDeliverMessageToHighestPriorityResourcesOnly(true);

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        stanzaRelay.setStanzaHandlerExecutorFactory(new SimpleStanzaHandlerExecutorFactory(stanzaRelay));

        TestSessionContext sessionContextTO_ENTITY_1_prio3 = createSessionForTo(TO_ENTITY, 3); // HIGHEST PRIO
        TestSessionContext sessionContextTO_ENTITY_2_prio0 = createSessionForTo(TO_ENTITY, 1); // not receiving
        TestSessionContext sessionContextTO_ENTITY_3_prio3 = createSessionForTo(TO_ENTITY, 3); // HIGHEST PRIO
        TestSessionContext sessionContextTO_ENTITY_4_prioMinus = createSessionForTo(TO_ENTITY, -1); // not receiving

        Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "Hello").build();

        try {
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, IgnoreFailureStrategy.INSTANCE);
            Stanza recordedStanza_1 = sessionContextTO_ENTITY_1_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 1 delivered", recordedStanza_1);
            Stanza recordedStanza_2 = sessionContextTO_ENTITY_2_prio0.getNextRecordedResponse(100);
            assertNull("stanza 2 not delivered", recordedStanza_2);
            Stanza recordedStanza_3 = sessionContextTO_ENTITY_3_prio3.getNextRecordedResponse(100);
            assertNotNull("stanza 3 delivered", recordedStanza_3);
            Stanza recordedStanza_4 = sessionContextTO_ENTITY_4_prioMinus.getNextRecordedResponse(100);
            assertNull("stanza 4 not delivered", recordedStanza_4);
        } catch (DeliveryException e) {
            throw e;
        }

    }

    private TestSessionContext createSessionForTo(EntityImpl TO_ENTITY, final int priority) {
        TestSessionContext sessionContextTO_ENTITY = TestSessionContext.createSessionContext(TO_ENTITY);
        sessionContextTO_ENTITY.setSessionState(SessionState.AUTHENTICATED);
        String TO_ENTITYRes = resourceRegistry.bindSession(sessionContextTO_ENTITY);
        resourceRegistry.setResourcePriority(TO_ENTITYRes, priority);
        return sessionContextTO_ENTITY;
    }

    public void testShutdown() throws DeliveryException {
        final ExecutorService testExecutorService = Executors.newFixedThreadPool(1);
        DeliveringInternalInboundStanzaRelay relay = new DeliveringInternalInboundStanzaRelay(testExecutorService);

        Assert.assertTrue(relay.isRelaying());
        relay.stop();
        Assert.assertFalse(relay.isRelaying());
        Assert.assertTrue(testExecutorService.isShutdown());

        Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "Hello").build();
        try {
            relay.relay(sessionContext, TO_ENTITY, stanza, null);
            Assert.fail("ServiceNotAvailableException expected");
        } catch (ServiceNotAvailableException e) {
            // test succeeds
        }
    }

    public void testSequentialDeliveryOneThread() throws DeliveryException, XMLSemanticError, EntityFormatException {

        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity,
                mock(StanzaRelay.class));

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        stanzaRelay.setStanzaHandlerExecutorFactory(new SimpleStanzaHandlerExecutorFactory(stanzaRelay));
        stanzaRelay.setMaxThreadCount(1);

        TestSessionContext sessionContext = createSessionForTo(TO_ENTITY, 1);

        final int STANZA_COUNT = 1000;

        for (int i = 0; i < STANZA_COUNT; i++) {
            Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "" + i).build();
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, null);
        }
        for (int i = 0; i < STANZA_COUNT; i++) {
            final Stanza nextResponse = sessionContext.getNextRecordedResponse(100);
            assertEquals("" + i, nextResponse.getSingleInnerElementsNamed("body").getSingleInnerText().getText());
        }

    }

    public void testSequentialDeliveryManyThreads() throws DeliveryException, XMLSemanticError, EntityFormatException {

        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity,
                mock(StanzaRelay.class));

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        stanzaRelay.setStanzaHandlerExecutorFactory(new SimpleStanzaHandlerExecutorFactory(stanzaRelay));
        stanzaRelay.setMaxThreadCount(10);

        TestSessionContext sessionContext = createSessionForTo(TO_ENTITY, 1);

        final int STANZA_COUNT = 1000;

        for (int i = 0; i < STANZA_COUNT; i++) {
            Stanza stanza = StanzaBuilder.createMessageStanza(FROM_ENTITY, TO_ENTITY, "en", "" + i).build();
            stanzaRelay.relay(sessionContext, TO_ENTITY, stanza, null);
        }

        /*
         * our handling currently does not enforce strict ordered delivery (for example
         * by session). If stanzas A and B enter the Relay's queue in that order, we
         * only ensure that processing is started in that order. However, it might as
         * well happen that B is processed in parallel to A, and even exits before A.
         */

        for (int i = 0; i < STANZA_COUNT; i++) {
            final Stanza nextResponse = sessionContext.getNextRecordedResponse(100);
            boolean matches = ("" + i)
                    .equals(nextResponse.getSingleInnerElementsNamed("body").getSingleInnerText().getText());
            if (!matches)
                return; // succeed
        }
        Assert.fail("delivered in sequence is unexpected, see VYSPER-337");
    }

}
