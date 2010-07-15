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
package org.apache.vysper.xmpp.extension.xep0124;

import static org.junit.Assert.*;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BoshHandlerTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @Ignore
    public void testProcess() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testGetSessionCreationStanza() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testWrapStanza() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testMergeStanzas() {
        fail("Not yet implemented");
    }

    private Stanza createPingRequestStanza(String to, String id) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        StanzaBuilder iq = body.startInnerElement("iq", NamespaceURIs.JABBER_CLIENT).addAttribute("to", to)
                .addAttribute("type", "get").addAttribute("id", id);
        iq.startInnerElement("ping", NamespaceURIs.URN_XMPP_PING).endInnerElement();
        iq.endInnerElement();
        body.endInnerElement();
        return body.build();
    }

    private static Stanza createPingResponseStanza(String from, String to, String id) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        body.startInnerElement("iq", NamespaceURIs.JABBER_CLIENT).addAttribute("from", from)
                .addAttribute("type", "result").addAttribute("to", to).addAttribute("id", id);
        body.endInnerElement();
        return body.build();
    }

}