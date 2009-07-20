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
package org.apache.vysper.xmpp.delivery;

import junit.framework.TestCase;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;

/**
 */
public class DeliveringStanzaRelayTestCase extends TestCase {
    
    protected ResourceRegistry resourceRegistry = new ResourceRegistry();
    protected AccountManagement accountVerification;
    protected DeliveringInboundStanzaRelay stanzaRelay;

    static class AccountVerificationMock implements AccountManagement {
        public void addUser(String username, String password) throws AccountCreationException {
            ; // empty
        }

        public boolean verifyAccountExists(Entity jid) {
            return true;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        accountVerification = new AccountVerificationMock(); 
        stanzaRelay = new DeliveringInboundStanzaRelay(resourceRegistry, accountVerification);
    }

    public void testSimpleRelay() throws EntityFormatException, XMLSemanticError, DeliveryException {
        EntityImpl fromEntity = EntityImpl.parse("userFrom@vysper.org");
        EntityImpl toEntity = EntityImpl.parse("userTo@vysper.org");
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(toEntity);
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        resourceRegistry.bindSession(sessionContext);

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").getFinalStanza();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            try { Thread.sleep(60); } catch (InterruptedException e) { ; } // eventually, this gets delivered
            Stanza recordedStanza = sessionContext.getNextRecordedResponse();
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

        Stanza stanza = StanzaBuilder.createMessageStanza(fromEntity, toEntity, "en", "Hello").getFinalStanza();

        try {
            stanzaRelay.relay(toEntity, stanza, new IgnoreFailureStrategy());
            try { Thread.sleep(60); } catch (InterruptedException e) { ; } // eventually, this gets delivered
            Stanza recordedStanza = sessionContext.getNextRecordedResponse();
            assertNull("stanza not delivered to unbound", recordedStanza);
        } catch (DeliveryException e) {
            throw e;
        }
    }
}
