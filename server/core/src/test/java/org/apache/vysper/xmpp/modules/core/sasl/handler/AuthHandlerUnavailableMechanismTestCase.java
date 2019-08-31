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

package org.apache.vysper.xmpp.modules.core.sasl.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.authentication.SASLMechanism;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.exception.AuthenticationFailedException;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import junit.framework.TestCase;

/**
 */
public class AuthHandlerUnavailableMechanismTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private RecordingStanzaBroker stanzaBroker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = new TestSessionContext(sessionStateHolder);
        sessionContext.setSessionState(SessionState.ENCRYPTED);

        List<SASLMechanism> methods = new ArrayList<SASLMechanism>();
        methods.add(new Plain());

        sessionContext.getServerRuntimeContext().getServerFeatures().setAuthenticationMethods(methods);

        stanzaBroker = new RecordingStanzaBroker();
    }

    public void testAuthPlainWrongCase() throws AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("auth", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        stanzaBuilder.addAttribute("mechanism", "plain"); // 'PLAIN' would be correct
        Stanza authPlainStanza = stanzaBuilder.build();

        AuthHandler authHandler = new AuthHandler();
        try {
            authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                    sessionStateHolder, stanzaBroker);

            fail("should raise exception");
        } catch (RuntimeException e) {
            // test succeeded
        }
    }

    public void testAuthPlainUnavailableMechanism() throws AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("auth", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        stanzaBuilder.addAttribute("mechanism", "EXTERNAL");
        Stanza authPlainStanza = stanzaBuilder.build();

        AuthHandler authHandler = new AuthHandler();
        try {
            authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                    sessionStateHolder, stanzaBroker);

            fail("should raise exception");
        } catch (RuntimeException e) {
            // test succeeded
        }
    }

}
