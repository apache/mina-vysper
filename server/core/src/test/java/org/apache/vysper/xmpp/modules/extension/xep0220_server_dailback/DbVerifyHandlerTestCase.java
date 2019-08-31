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
package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.RecordingStanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DbVerifyHandlerTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from.org");

    private static final Entity TO = EntityImpl.parseUnchecked("to.org");

    private static final String ID = "D60000229F";

    private DbVerifyHandler handler = new DbVerifyHandler();

    private RecordingStanzaBroker stanzaBroker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        stanzaBroker = new RecordingStanzaBroker();
    }

    public void testVerify() {
        Stanza correct = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK).build();
        Stanza invalidNamespace = new StanzaBuilder("verify", "dummy").build();
        Stanza invalidName = new StanzaBuilder("dummy", NamespaceURIs.JABBER_SERVER_DIALBACK).build();

        Assert.assertTrue(handler.verify(correct));
        Assert.assertFalse(handler.verify(invalidNamespace));
        Assert.assertFalse(handler.verify(invalidName));

    }

    public void testExecuteValidVerification() {
        String token = new DialbackIdGenerator().generate(FROM, TO, ID);
        assertExecuteVerification(token, "valid");
    }

    public void testExecuteInvalidVerification() {
        assertExecuteVerification("12345", "invalid");
    }

    private void assertExecuteVerification(String token, String expectedType) {
        ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
        Mockito.when(serverRuntimeContext.getServerEntity()).thenReturn(TO);

        Stanza stanza = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK)
                .addAttribute("from", FROM.getFullQualifiedName()).addAttribute("to", TO.getFullQualifiedName())
                .addAttribute("id", ID).addText(token).build();

        handler.execute(stanza, serverRuntimeContext, false, null, null, stanzaBroker);

        Stanza response = stanzaBroker.getUniqueStanzaWrittenToSession();

        Assert.assertNotNull(response);
        Assert.assertEquals(TO, response.getFrom());
        Assert.assertEquals(FROM, response.getTo());
        Assert.assertEquals(ID, response.getAttributeValue("id"));
        Assert.assertEquals(expectedType, response.getAttributeValue("type"));
    }

}
