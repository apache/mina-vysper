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
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * test session initiation bevahior
 */
public class ProtocolInitiatedTestCase extends AbstractProtocolStateTestCase {

    public void testProcessClientCanonicalStreamOpeningResponse() {

        sessionContext.setXMLLang("fr");
        openClientSession();

        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        XMLElementVerifier responseVerifier = recordedResponse.getVerifier();

        assertTrue(responseVerifier.nameEquals("stream"));

        assertTrue(responseVerifier.attributeEquals(NamespaceURIs.XML, "lang", "fr"));

        assertTrue("initiated => started", sessionContext.getState() == SessionState.STARTED);
    }

    public void testProcessClientStreamOpeningResponse_XMLLang_fr() {

        sessionContext.setSessionState(getDefaultState());

        // french in, french returned
        checkLanguage("fr");
    }

    @Override
    protected SessionState getDefaultState() {
        return SessionState.INITIATED;
    }

    public void testProcessClientStreamOpeningResponse_XMLLang_null() {

        sessionContext.setSessionState(getDefaultState());

        // no lang in, no lang returned
        checkLanguage(null);
    }

    public void testProcessClientStreamOpeningResponse_XMLLang_enUS() {

        sessionContext.setSessionState(getDefaultState());

        // US-english in, US-english returned
        checkLanguage("en_US");
    }

    protected void openClientSession() {
        sessionContext.setSessionState(getDefaultState());
        Stanza stanza = new ServerResponses().getStreamOpener(true, testFrom, sessionContext.getXMLLang(),
                XMPPVersion.VERSION_1_0, null).build();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);
    }

    public void testProcessClientStreamOpeningResponse_Version_1_0() {
        sessionContext.setSessionState(getDefaultState());

        XMPPVersion versionSent = XMPPVersion.VERSION_1_0;
        Stanza response = getVersionResponse(versionSent);

        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.attributeEquals("version", XMPPVersion.VERSION_1_0.toString()));
        assertTrue(responseVerifier.attributePresent("id"));
        assertFalse("no error", responseVerifier.subElementPresent("error"));
    }

    public void testProcessClientStreamOpeningResponse_NoVersion() {
        sessionContext.setSessionState(getDefaultState());

        XMPPVersion versionSent = null;
        Stanza response = getVersionResponse(versionSent);

        XMLElementVerifier responseVerifier = response.getVerifier();
        assertFalse(responseVerifier.attributePresent("version"));
        assertFalse("no error", responseVerifier.subElementPresent("error"));
    }

    public void testProcessClientStreamOpeningResponse_Version_1_1() {
        sessionContext.setSessionState(getDefaultState());

        XMPPVersion versionSent = new XMPPVersion(1, 1);
        Stanza response = getVersionResponse(versionSent);

        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.attributeEquals("version", XMPPVersion.VERSION_1_0.toString()));
        assertFalse("no error", responseVerifier.subElementPresent("error"));
    }

    protected static class IllegalXMPPVersion extends XMPPVersion {
        protected String versionString;

        public IllegalXMPPVersion(String version) {
            versionString = version;
        }

        @Override
        public String toString() {
            return versionString;
        }
    }

    public void testProcessClientStreamOpeningResponse_IllegalVersion() {
        sessionContext.setSessionState(getDefaultState());

        XMPPVersion versionSent = new IllegalXMPPVersion("IllV1.0");
        Stanza response = getVersionResponse(versionSent);

        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.nameEquals("error"));
        assertTrue("error", responseVerifier.subElementPresent(StreamErrorCondition.UNSUPPORTED_VERSION.value()));
    }

    public void testProcessClientStreamOpeningResponse_Version_2_0() {
        sessionContext.setSessionState(getDefaultState());

        XMPPVersion versionSent = new XMPPVersion(2, 0);
        Stanza response = getVersionResponse(versionSent);

        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.nameEquals("error"));
        assertTrue("error", responseVerifier.subElementPresent(StreamErrorCondition.UNSUPPORTED_VERSION.value()));
    }

    protected Stanza getVersionResponse(XMPPVersion versionSent) {
        Stanza stanza = new ServerResponses().getStreamOpener(true, testFrom, null, versionSent, null).build();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        return sessionContext.getNextRecordedResponse();
    }

    public void testProcessClientStreamOpeningResponse_MissingMainNamespace() {
        // we do not supply "http://etherx.jabber.org/streams"
        StanzaBuilder stanzaBuilder = new StanzaBuilder("stream").declareNamespace("", NamespaceURIs.JABBER_CLIENT)
                .addAttribute(NamespaceURIs.XML, "lang", "en_UK").addAttribute("version",
                        XMPPVersion.VERSION_1_0.toString());
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        Stanza response = sessionContext.getNextRecordedResponse();
        XMLElementVerifier responseVerifier = response.getVerifier();
        assertTrue(responseVerifier.nameEquals("error"));
        assertTrue("error", responseVerifier.subElementPresent(StreamErrorCondition.INVALID_NAMESPACE.value()));

    }

    public void testDontAcceptIQStanzaWhileNotAuthenticated() {
        skeleton_testDontAcceptIQStanzaWhileNotAuthenticated();
    }

    public void testDontAcceptArbitraryStanzaWhileNotAuthenticated() {
        skeleton_testDontAcceptArbitraryStanzaWhileNotAuthenticated();
    }

}
