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

package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;

import junit.framework.TestCase;

/**
 * test basic behavior of ProtocolWorker.aquireStanza()
 */
public class ProtocolWorkerStateAwarenessTestCase extends TestCase {
    private ProtocolWorker protocolWorker;

    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sessionStateHolder = new SessionStateHolder();
        Entity serverEnitity = new EntityImpl(null, "vysper-server.org", null);
        StanzaReceiverRelay receiverRelay = new StanzaReceiverRelay();
        protocolWorker = new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(receiverRelay));
        DefaultServerRuntimeContext serverRuntimeContext = new DefaultServerRuntimeContext(serverEnitity,
                receiverRelay);
        receiverRelay.setServerRuntimeContext(serverRuntimeContext);
        serverRuntimeContext.addDictionary(new BaseStreamStanzaDictionary());
        sessionStateHolder = new SessionStateHolder();
        sessionContext = new TestSessionContext(serverRuntimeContext, sessionStateHolder, receiverRelay);
    }

    public void testNotAcceptRegularStanzaBeyondAuthenticatedState_IQ() throws Exception {

        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "get");
        Stanza stanza = stanzaBuilder.build();

        assertNotAuthorized(stanza);
    }

    public void testNotAcceptRegularStanzaBeyondAuthenticatedState_Presence() throws Exception {

        StanzaBuilder stanzaBuilder = new StanzaBuilder("presence", NamespaceURIs.JABBER_CLIENT);
        // TODO? add more presence specifics
        Stanza stanza = stanzaBuilder.build();

        assertNotAuthorized(stanza);
    }

    public void testNotAcceptRegularStanzaBeyondAuthenticatedState_Message() throws Exception {

        StanzaBuilder stanzaBuilder = new StanzaBuilder("message", NamespaceURIs.JABBER_CLIENT);
        // TODO? add more message specifics
        Stanza stanza = stanzaBuilder.build();

        assertNotAuthorized(stanza);
    }

    private void assertNotAuthorized(Stanza stanza) throws Exception {
        SessionState[] allStates = SessionState.values();
        for (SessionState state : allStates) {
            if (state == SessionState.AUTHENTICATED)
                continue; // skip allowed state

            setUp();

            sessionStateHolder.setState(state);
            protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                    sessionStateHolder);

            Stanza response = sessionContext.getNextRecordedResponse();
            XMLElementVerifier xmlElementVerifier = response.getVerifier();

            // RFC3920/4.3: response must be "not-authorized"
            assertTrue("error stanza", xmlElementVerifier.nameEquals("error"));
            assertTrue("error stanza not-authorized",
                    xmlElementVerifier.subElementPresent(StanzaErrorCondition.NOT_AUTHORIZED.value()));
            assertTrue("writer had been closed", sessionContext.isClosed());
            assertEquals("session closed", SessionState.CLOSED, sessionContext.getState());
        }
    }

}
