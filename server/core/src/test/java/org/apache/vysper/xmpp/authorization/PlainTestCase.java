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
package org.apache.vysper.xmpp.authorization;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class PlainTestCase extends TestCase {
    protected SessionStateHolder stateHolder;

    public void testPlainEmpty() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).build();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    public void testPlainNonBASE64() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText(
                "aEflkejidkj==").build();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    public void testPlainNonExistingUser() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText(
                encode("dieter", "schluppkoweit")).build();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "not-authorized");
    }

    public void testPlainNotExistingUser() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText(
                encode("dieter", "schluppkoweit")).build();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "not-authorized");
    }

    public void testPlainNoUserPasswordCombination() throws XMLSemanticError {

        String innerText = new String(Base64.encodeBase64("continuous".getBytes()));

        Stanza stanza = new StanzaBuilder("plain", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText(innerText)
                .build();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    private Stanza startMechanism(Stanza finalStanza) {
        Plain plain = new Plain();
        stateHolder = new SessionStateHolder();
        Stanza response = plain.started(new TestSessionContext(stateHolder), stateHolder, finalStanza);
        return response;
    }

    private void assertResponse(Stanza response, String failureType) throws XMLSemanticError {
        assertTrue(response.getVerifier().nameEquals("failure"));
        assertNotNull(response.getSingleInnerElementsNamed(failureType));
        assert stateHolder.getState() != SessionState.AUTHENTICATED;
    }
    
    public static void main(String[] args) {
        PlainTestCase plain = new PlainTestCase();
        System.out.println(plain.encode("user1@vysper.org", "password1"));
    }

    private String encode(String username, String password) {
        return new String(Base64.encodeBase64(('\0' + username + '\0' + password).getBytes()));
    }
}
