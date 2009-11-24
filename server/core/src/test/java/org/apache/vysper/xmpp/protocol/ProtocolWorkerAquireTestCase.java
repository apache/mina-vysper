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

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.parser.StreamParser;
import org.apache.vysper.xmpp.parser.StringStreamParser;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * test basic behavior of ProtocolWorker.aquireStanza()
 */
public class ProtocolWorkerAquireTestCase extends TestCase {
    private ProtocolWorker protocolWorker;
    private TestSessionContext sessionContext;
    private SessionStateHolder sessionStateHolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        protocolWorker = new ProtocolWorker();
        sessionStateHolder = new SessionStateHolder();
        StanzaReceiverRelay receiverRelay = new StanzaReceiverRelay();
        DefaultServerRuntimeContext context = new DefaultServerRuntimeContext(new EntityImpl(null, "test", null), receiverRelay);
        receiverRelay.setServerRuntimeContext(context);
        sessionContext = new TestSessionContext(context, sessionStateHolder);
    }

    public void testAquireSimpleStanza() {

        StreamParser streamParser = new StringStreamParser("<ProtocolHandlerTestStanzaHandler xmlns='testNSURI' ></ProtocolHandlerTestStanzaHandler>");
        Stanza stanza = protocolWorker.aquireStanza(sessionContext, streamParser);
        assertNotNull(stanza);

        Stanza expectedStanza = new StanzaBuilder("ProtocolHandlerTestStanzaHandler", "testNSURI").addNamespaceAttribute("testNSURI").build();
        assertEquals("stanza full match", expectedStanza, stanza);

    }

    public void testAquireXMLNotWellformedStanza() {

        StreamParser streamParser = new StringStreamParser("<ProtocolHandlerTestStanzaHandler><inner_not_closed></ProtocolHandlerTestStanzaHandler>");
        Stanza stanza = protocolWorker.aquireStanza(sessionContext, streamParser);
        assertNull(stanza);

        assertErrorResponse();

    }

    public void testAquireXMLIncomplete() {

        StreamParser streamParser = new StringStreamParser("<ProtocolHandlerTestStanzaHandler></Pro");
        Stanza stanza = protocolWorker.aquireStanza(sessionContext, streamParser);
        assertNull(stanza);

        assertErrorResponse();

    }

    private void assertErrorResponse() {
        assertTrue("session closed", sessionContext.isClosed());
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertEquals("error reported", "error", recordedResponse.getName());
        assertTrue("bad format", recordedResponse.getVerifier().subElementPresent("bad-format"));
    }

    public void testAquireNoNextStanza() {

        StreamParser streamParser = new StringStreamParser("<ProtocolHandlerTestStanzaHandler ></ProtocolHandlerTestStanzaHandler>");
        Stanza stanza = protocolWorker.aquireStanza(sessionContext, streamParser);
        assertNotNull(stanza);

        stanza = protocolWorker.aquireStanza(sessionContext, streamParser);
        assertNull("no next", stanza);
        assertFalse("not closed", sessionContext.isClosed());
    }

}
