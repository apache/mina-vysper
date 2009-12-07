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

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.authorization.SimpleUserAuthorization;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.exception.AuthorizationFailedException;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;
import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.commons.codec.binary.Base64;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AbortHandlerTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = new TestSessionContext(sessionStateHolder);
        sessionContext.setSessionState(SessionState.ENCRYPTED);

        List<SASLMechanism> methods = new ArrayList<SASLMechanism>();
        methods.add(new Plain());
        
        sessionContext.getServerRuntimeContext().getServerFeatures().setAuthenticationMethods(methods);
        SimpleUserAuthorization users = new SimpleUserAuthorization();
        users.addUser("user007@test", "pass007");
        OpenStorageProviderRegistry providerRegistry = new OpenStorageProviderRegistry();
        providerRegistry.add(users);
        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext()).setStorageProviderRegistry(providerRegistry);
    }
    public void testAbort() throws XMLSemanticError, AuthorizationFailedException {

        executeAbortAuthorization_3Times();
        
        StanzaBuilder stanzaBuilder = createAbort();
        Stanza abortPlainStanza = stanzaBuilder.build();

        stanzaBuilder = new StanzaBuilder("auth", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        stanzaBuilder.addAttribute("mechanism", "PLAIN");
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user007\0pass007".getBytes())));
        Stanza authPlainStanza = stanzaBuilder.build();

        // correct credential no longer work - no retries left
        AuthHandler authHandler = new AuthHandler();
        try {
            ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
            fail("should raise error - no tries left");
        } catch (AuthorizationFailedException e) {
            // test succeeded
        }

    }

    private void executeAbortAuthorization_3Times() throws AuthorizationFailedException {
        Stanza responseStanza = executeAbort();
        assertTrue(responseStanza.getVerifier().nameEquals("aborted"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(2, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        responseStanza = executeAbort();
        assertTrue(responseStanza.getVerifier().nameEquals("aborted"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(1, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        responseStanza = executeAbort();
        assertTrue(responseStanza.getVerifier().nameEquals("aborted"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(0, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());
    }

    private Stanza executeAbort() throws AuthorizationFailedException {
        StanzaBuilder stanzaBuilder = createAbort();

        Stanza abortStanza = stanzaBuilder.build();

        AbortHandler abortHandler = new AbortHandler();
        ResponseStanzaContainer responseContainer = abortHandler.execute(abortStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
        Stanza responseStanza = responseContainer.getResponseStanza();
        return responseStanza;
    }

    private StanzaBuilder createAbort() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("abort");
        stanzaBuilder.addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        return stanzaBuilder;
    }


}
