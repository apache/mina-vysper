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

package org.apache.vysper.xmpp.server.response;

import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 */
public class ServerErrorResponsesTestCase extends TestCase {

    private static final String SERVER_JID = "vysper.org";

    private static final String CLIENT_JID = "test@vysper.org/test";

    public void testStanzaError() throws ParsingException {
        StanzaBuilder builder = new StanzaBuilder("iq");
        builder.addAttribute("to", SERVER_JID);
        builder.addAttribute("from", CLIENT_JID);
        builder.startInnerElement("ping", NamespaceURIs.URN_XMPP_PING);
        builder.endInnerElement();

        Stanza request = builder.build();
        IQStanza requestIq = new IQStanza(request);

        Stanza errorReply = ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE, requestIq,
                StanzaErrorType.CANCEL, "Test", "en", null);
        assertEquals("error", errorReply.getAttributeValue("type"));
        assertEquals(SERVER_JID, errorReply.getAttributeValue("from"));
        assertEquals(CLIENT_JID, errorReply.getAttributeValue("to"));

        List<XMLElement> children = errorReply.getInnerElements();

        XMLElement pingElm = children.get(0);
        assertEquals("ping", pingElm.getName());
        assertEquals(NamespaceURIs.URN_XMPP_PING, pingElm.getNamespaceURI());

        XMLElement errorElm = children.get(1);
        assertEquals("error", errorElm.getName());
        assertEquals(StanzaErrorType.CANCEL.value(), errorElm.getAttributeValue("type"));

        XMLElement errorTypeElm = errorElm.getInnerElements().get(0);
        assertEquals(StanzaErrorCondition.SERVICE_UNAVAILABLE.value(), errorTypeElm.getName());

        XMLElement textElm = errorElm.getInnerElements().get(1);
        assertEquals("text", textElm.getName());
        assertEquals("en", textElm.getXMLLang());
        assertEquals("Test", textElm.getFirstInnerText().getText());
    }
}
