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
package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import junit.framework.TestCase;

/**
 */
public abstract class AbstractProtocolStateTestCase extends TestCase {
    protected ProtocolWorker protocolWorker;

    protected DefaultServerRuntimeContext serverRuntimeContext;

    protected TestSessionContext sessionContext;

    protected static Entity serverEnitity = new EntityImpl(null, "vysper-server.org", null);

    protected EntityImpl testFrom = new EntityImpl("testuser", "vysper.org", null);

    protected SessionStateHolder sessionStateHolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        StanzaReceiverRelay receiverRelay = new StanzaReceiverRelay();
        protocolWorker = new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(receiverRelay));
        serverRuntimeContext = new DefaultServerRuntimeContext(serverEnitity, receiverRelay);
        receiverRelay.setServerRuntimeContext(serverRuntimeContext);
        serverRuntimeContext.addDictionary(new BaseStreamStanzaDictionary());
        sessionStateHolder = new SessionStateHolder();
        sessionContext = new TestSessionContext(serverRuntimeContext, sessionStateHolder, receiverRelay);
        sessionContext.setSessionState(getDefaultState());
    }

    protected void checkLanguage(String xmlLang) {
        Stanza stanza;
        Stanza recordedResponse;
        XMLElementVerifier responseVerifier;
        stanza = new ServerResponses().getStreamOpener(true, testFrom, xmlLang, XMPPVersion.VERSION_1_0, null).build();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        recordedResponse = sessionContext.getNextRecordedResponse();
        responseVerifier = recordedResponse.getVerifier();
        if (xmlLang == null) {
            assertFalse(responseVerifier.attributePresent(NamespaceURIs.XML, "lang"));
        } else {
            assertTrue(responseVerifier.attributeEquals(NamespaceURIs.XML, "lang", xmlLang));
        }
    }

    protected abstract SessionState getDefaultState();

    /**
     * call from a test method in subclass
     */
    protected void skeleton_testDontAcceptIQStanzaWhileNotAuthenticated() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "get");

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        Stanza response = sessionContext.getNextRecordedResponse();
        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.nameEquals("error"));
        assertTrue("error", responseVerifier.subElementPresent(StreamErrorCondition.NOT_AUTHORIZED.value()));
        assertTrue(sessionContext.isClosed());
    }

    /**
     * call from a test method in subclass
     */
    protected void skeleton_testDontAcceptArbitraryStanzaWhileNotAuthenticated() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("arbitrary", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "get");

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        Stanza response = sessionContext.getNextRecordedResponse();

        XMLElementVerifier responseVerifier = response.getVerifier();
        // error might be wrapped within a stream opener
        if (responseVerifier.nameEquals("stream")) {
            responseVerifier = response.getFirstInnerElement().getVerifier();
        }

        assertTrue(responseVerifier.nameEquals("error"));
        assertTrue("error", responseVerifier.subElementPresent(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE.value()));
        assertTrue(sessionContext.isClosed());
    }
}
