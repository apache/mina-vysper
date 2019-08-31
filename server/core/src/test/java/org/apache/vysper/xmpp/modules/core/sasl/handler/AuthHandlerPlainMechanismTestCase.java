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

import org.apache.commons.codec.binary.Base64;
import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.authentication.SASLMechanism;
import org.apache.vysper.xmpp.authentication.SimpleUserAuthentication;
import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.exception.AuthenticationFailedException;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import junit.framework.TestCase;

/**
 */
public class AuthHandlerPlainMechanismTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sessionContext = new TestSessionContext(sessionStateHolder);
        sessionContext.setSessionState(SessionState.ENCRYPTED);

        List<SASLMechanism> methods = new ArrayList<>();
        methods.add(new Plain());

        sessionContext.getServerRuntimeContext().getServerFeatures().setAuthenticationMethods(methods);
        SimpleUserAuthentication users = new SimpleUserAuthentication();
        users.addUser(EntityImpl.parseUnchecked("user007@test"), "pass007");
        OpenStorageProviderRegistry providerRegistry = new OpenStorageProviderRegistry();
        providerRegistry.add(users);
        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext())
                .setStorageProviderRegistry(providerRegistry);
    }

    public void testAuthPlainNoInitialResponse() throws AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        Stanza authPlainStanza = stanzaBuilder.build();

        AuthHandler authHandler = new AuthHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);

        assertTrue(stanzaBroker.getUniqueStanzaWrittenToSession().getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
    }

    public void testAuthPlainEmptyInitialResponse() throws AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText("=");
        Stanza authPlainStanza = stanzaBuilder.build();

        AuthHandler authHandler = new AuthHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);

        assertTrue(stanzaBroker.getUniqueStanzaWrittenToSession().getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
    }

    public void testAuthPlainAuthorizedCredentialsResponse() throws XMLSemanticError, AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user007\0pass007".getBytes())));

        Stanza authPlainStanza = stanzaBuilder.build();

        assertEquals(3, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        AuthHandler authHandler = new AuthHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);
        Stanza responseStanza = stanzaBroker.getUniqueStanzaWrittenToSession();

        assertTrue(responseStanza.getVerifier().nameEquals("success"));
        assertTrue(sessionStateHolder.getState() == SessionState.AUTHENTICATED);
        assertNull(sessionContext.getAttribute(AuthorizationRetriesCounter.SESSION_ATTRIBUTE_ABORTION_COUNTER));
    }

    public void testAuthPlainWrongCredentialsResponse() throws XMLSemanticError, AuthenticationFailedException {

        executeWrongPlainAuthorization_3Times();

        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user007\0pass007".getBytes())));
        Stanza authPlainStanza = stanzaBuilder.build();

        // correct credential no longer work - no retries left
        AuthHandler authHandler = new AuthHandler();
        try {
            RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
            authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                    sessionStateHolder, stanzaBroker);
            fail("should raise error - no tries left");
        } catch (AuthenticationFailedException e) {
            // test succeeded
        }

    }

    private void executeWrongPlainAuthorization_3Times() throws AuthenticationFailedException {
        Stanza responseStanza = executeWrongPlainAuthorization();
        assertTrue(responseStanza.getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(2, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        responseStanza = executeWrongPlainAuthorization();
        assertTrue(responseStanza.getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(1, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        responseStanza = executeWrongPlainAuthorization();
        assertTrue(responseStanza.getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
        assertEquals(0, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());
    }

    private Stanza executeWrongPlainAuthorization() throws AuthenticationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user008\0pass007".getBytes())));

        Stanza authPlainStanza = stanzaBuilder.build();

        AuthHandler authHandler = new AuthHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);
        return stanzaBroker.getUniqueStanzaWrittenToSession();
    }

    private StanzaBuilder createAuthPlain() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("auth", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        stanzaBuilder.addAttribute("mechanism", "PLAIN");
        return stanzaBuilder;
    }

}
