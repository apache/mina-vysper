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

package org.apache.vysper.xmpp.modules.core.starttls.handler;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import junit.framework.TestCase;

/**
 */
public class StartTLSHandlerTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = new TestSessionContext(sessionStateHolder);
    }

    public void testAppropriateSessionState() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        Stanza starttlsStanza = stanzaBuilder.build();

        TestSessionContext sessionContext = this.sessionContext;
        sessionContext.setServerToServer();

        sessionContext.setSessionState(SessionState.INITIATED);
        Stanza responseStanza = executeStartTLSHandler(starttlsStanza, sessionContext);
        XMLElementVerifier verifier = responseStanza.getVerifier();
        assertTrue("session state to low failure", verifier.nameEquals("failure"));
        assertFalse("tls init", sessionContext.isSwitchToTLSCalled());

        sessionContext.setSessionState(SessionState.ENCRYPTION_STARTED);
        responseStanza = executeStartTLSHandler(starttlsStanza, sessionContext);
        verifier = responseStanza.getVerifier();
        assertTrue("session state too high failure", verifier.nameEquals("failure"));
        assertFalse("tls init", sessionContext.isSwitchToTLSCalled());

        sessionContext.setSessionState(SessionState.STARTED);
        responseStanza = executeStartTLSHandler(starttlsStanza, sessionContext);
        verifier = responseStanza.getVerifier();
        assertTrue("session state ready", verifier.nameEquals("proceed"));
        assertEquals("session stat is encryption started", SessionState.ENCRYPTION_STARTED,
                sessionStateHolder.getState());
        assertTrue("tls init", sessionContext.isSwitchToTLSCalled());
    }

    private Stanza executeStartTLSHandler(Stanza starttlsStanza, TestSessionContext sessionContext) {
        StartTLSHandler startTLSHandler = new StartTLSHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        startTLSHandler.execute(starttlsStanza, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, stanzaBroker);
        return stanzaBroker.getUniqueStanzaWrittenToSession();
    }

    public void testNamespace() {
        sessionContext.setSessionState(SessionState.STARTED);

        StanzaBuilder stanzaBuilder = new StanzaBuilder("starttls", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS);
        Stanza wrongNSStanza = stanzaBuilder.build();

        stanzaBuilder = new StanzaBuilder("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        Stanza correctNSStanza = stanzaBuilder.build();

        TestSessionContext sessionContext = this.sessionContext;

        Stanza responseStanza = executeStartTLSHandler(wrongNSStanza, sessionContext);
        XMLElementVerifier verifier = responseStanza.getVerifier();
        assertTrue("namespace wrong failure", verifier.nameEquals("failure"));
        assertFalse("tls init", sessionContext.isSwitchToTLSCalled());

        responseStanza = executeStartTLSHandler(correctNSStanza, sessionContext);
        verifier = responseStanza.getVerifier();
        assertTrue("namespace correct proceed", verifier.nameEquals("proceed"));
        assertTrue("tls init", sessionContext.isSwitchToTLSCalled());
    }

}
