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

package org.apache.vysper.xmpp.modules.core.sasl.handler;

import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.authorization.SimpleUserAuthorization;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.exception.AuthorizationFailedException;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;

import java.util.ArrayList;
import java.util.List;

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

        List<SASLMechanism> methods = new ArrayList<SASLMechanism>();
        methods.add(new Plain());
        
        sessionContext.getServerRuntimeContext().getServerFeatures().setAuthenticationMethods(methods);
        SimpleUserAuthorization users = new SimpleUserAuthorization();
        users.addUser("user007@test", "pass007");
        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext()).setUserAuthorization(users);
    }

    public void testAuthPlainNoInitialResponse() throws AuthorizationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        Stanza authPlainStanza = stanzaBuilder.getFinalStanza();

        AuthHandler authHandler = new AuthHandler();
        ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);

        assertTrue(responseContainer.getResponseStanza().getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
    }

    public void testAuthPlainEmptyInitialResponse() throws AuthorizationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText("=");
        Stanza authPlainStanza = stanzaBuilder.getFinalStanza();

        AuthHandler authHandler = new AuthHandler();
        ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);

        assertTrue(responseContainer.getResponseStanza().getVerifier().nameEquals("failure"));
        assertTrue(sessionStateHolder.getState() == SessionState.ENCRYPTED);
    }

    public void testAuthPlainAuthorizedCredentialsResponse() throws XMLSemanticError, AuthorizationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user007\0pass007".getBytes())));
        
        Stanza authPlainStanza = stanzaBuilder.getFinalStanza();

        assertEquals(3, AuthorizationRetriesCounter.getFromSession(sessionContext).getTriesLeft());

        AuthHandler authHandler = new AuthHandler();
        ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
        Stanza responseStanza = responseContainer.getResponseStanza();
        
        assertTrue(responseStanza.getVerifier().nameEquals("success"));
        assertTrue(sessionStateHolder.getState() == SessionState.AUTHENTICATED);
        assertNull(sessionContext.getAttribute(AuthorizationRetriesCounter.SESSION_ATTRIBUTE_ABORTION_COUNTER));
    }

    public void testAuthPlainWrongCredentialsResponse() throws XMLSemanticError, AuthorizationFailedException {

        executeWrongPlainAuthorization_3Times();
        
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user007\0pass007".getBytes())));
        Stanza authPlainStanza = stanzaBuilder.getFinalStanza();

        // correct credential no longer work - no retries left
        AuthHandler authHandler = new AuthHandler();
        try {
            ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
            fail("should raise error - no tries left");
        } catch (AuthorizationFailedException e) {
            // test succeeded
        }

    }

    private void executeWrongPlainAuthorization_3Times() throws AuthorizationFailedException {
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

    private Stanza executeWrongPlainAuthorization() throws AuthorizationFailedException {
        StanzaBuilder stanzaBuilder = createAuthPlain();
        stanzaBuilder.addText(new String(Base64.encodeBase64("dummy\0user008\0pass007".getBytes())));

        Stanza authPlainStanza = stanzaBuilder.getFinalStanza();

        AuthHandler authHandler = new AuthHandler();
        ResponseStanzaContainer responseContainer = authHandler.execute(authPlainStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
        Stanza responseStanza = responseContainer.getResponseStanza();
        return responseStanza;
    }

    private StanzaBuilder createAuthPlain() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("auth");
        stanzaBuilder.addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        stanzaBuilder.addAttribute("mechanism", "PLAIN");
        return stanzaBuilder;
    }

}
