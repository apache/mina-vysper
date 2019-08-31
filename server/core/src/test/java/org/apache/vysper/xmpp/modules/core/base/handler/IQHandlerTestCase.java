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

package org.apache.vysper.xmpp.modules.core.base.handler;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanzaVerifier;

import junit.framework.TestCase;

/**
 */
public class IQHandlerTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = new TestSessionContext(sessionStateHolder);
    }

    public void testMissingToInServerCall() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_SERVER);
        stanzaBuilder.addAttribute("type", "get");
        // missing stanzaBuilder.addAttribute("to", "test@example.com");
        stanzaBuilder.addAttribute("id", "anyway");
        stanzaBuilder.startInnerElement("inner", NamespaceURIs.JABBER_SERVER).endInnerElement();

        TestSessionContext sessionContext = this.sessionContext;
        sessionContext.setServerToServer();

        TestIQHandler iqHandler = new TestIQHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        iqHandler.execute(stanzaBuilder.build(), sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker);
        Stanza responseStanza = stanzaBroker.getUniqueStanzaWrittenToSession();
        XMLElementVerifier verifier = responseStanza.getVerifier();
        assertTrue("error", verifier.nameEquals("error"));
    }

    public void testMissingID() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("type", "get");
        assertIQError(stanzaBuilder.build());
    }

    public void testDoNotRespondToErrorWithError() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("type", "error");
        Stanza stanza = stanzaBuilder.build(); // this stanza has no ID

        IQHandler iqHandler = new IQHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        iqHandler.execute(stanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null, stanzaBroker);
        Stanza responseStanza = stanzaBroker.getUniqueStanzaWrittenToSession();
        XMLElementVerifier verifier = responseStanza.getVerifier();
        assertTrue("error", verifier.nameEquals("error")); // response is _not_ IQ stanza
    }

    private void assertIQError(Stanza stanza) {
        TestIQHandler iqHandler = new TestIQHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        iqHandler.execute(stanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null, stanzaBroker);
        Stanza responseStanza = stanzaBroker.getUniqueStanzaWrittenToSession();
        XMLElementVerifier verifier = responseStanza.getVerifier();
        assertTrue("iq", verifier.nameEquals("iq"));
        assertTrue("error type", verifier.attributeEquals("type", IQStanzaType.ERROR.value()));
        assertTrue("iq-error", verifier.subElementPresent("error"));
    }

    public void testMissingType() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        // missing: stanzaBuilder.addAttribute("type", "get");
        assertIQError(stanzaBuilder.build());
    }

    public void testUnsupportedType() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "bogus");
        assertIQError(stanzaBuilder.build());
    }

    public void testGetAndSetSubelements() {
        // get and set must have exactly one subelement

        String type = "get";
        assertAnySub(type); // test with zero
        assertNotTwoSubs(type); // test with 2

        type = "set";
        assertAnySub(type);
        assertNotTwoSubs(type);
    }

    public void testResultSubelements() {
        // result must have zero or one subelements
        String type = "result";
        assertNotTwoSubs(type); // test with two
    }

    private void assertNotTwoSubs(String type) {
        StanzaBuilder stanzaTwoSubs = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaTwoSubs.addAttribute("id", "1");
        stanzaTwoSubs.addAttribute("type", type);
        stanzaTwoSubs.startInnerElement("firstSub", NamespaceURIs.JABBER_CLIENT).endInnerElement();
        stanzaTwoSubs.startInnerElement("secondSub", NamespaceURIs.JABBER_CLIENT).endInnerElement();
        assertIQError(stanzaTwoSubs.build());
    }

    private void assertAnySub(String type) {
        StanzaBuilder stanzaNoSub = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaNoSub.addAttribute("id", "1");
        stanzaNoSub.addAttribute("type", type);
        assertIQError(stanzaNoSub.build());
    }

    public void testGet() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "get");
        stanzaBuilder.startInnerElement("getRequest", NamespaceURIs.JABBER_CLIENT).endInnerElement();

        TestIQHandler iqHandler = new TestIQHandler();
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        iqHandler.execute(stanzaBuilder.build(), sessionContext.getServerRuntimeContext(), true, sessionContext, null,
                stanzaBroker);
        IQStanza incomingStanza = iqHandler.getIncomingStanza();

        XMPPCoreStanzaVerifier verifier = incomingStanza.getCoreVerifier();
        assertTrue("iq", verifier.nameEquals("iq"));
        assertTrue("iq-id", verifier.attributeEquals("id", "1"));
        assertTrue("iq-type-get", verifier.attributeEquals("type", "get"));

        // response is "result"
        Stanza responseStanza = stanzaBroker.getUniqueStanzaWrittenToSession();
        XMLElementVerifier responseVerifier = responseStanza.getVerifier();
        assertTrue("iq", responseVerifier.nameEquals("iq"));
        assertTrue("iq-id", responseVerifier.attributeEquals("id", "1"));
        assertTrue("iq-type-result", responseVerifier.attributeEquals("type", "result"));
    }

}
