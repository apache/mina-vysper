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

package org.apache.vysper.xmpp.modules.core.base.handler;

import junit.framework.TestCase;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.delivery.*;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.xmlfragment.XMLElementVerifier;

/**
 */
public class MessageHandlerRelayTestCase extends TestCase {
    private TestSessionContext sessionContext;

    private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    private MessageHandler messageHandler = new MessageHandler();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionContext = TestSessionContext.createWithStanzaReceiverRelay(sessionStateHolder);
    }

    public void testStanzaRelayed() {
        sessionContext = new TestSessionContext(sessionStateHolder);
        
        String timestamp = "" + System.currentTimeMillis();

        StanzaBuilder stanzaBuilder = new StanzaBuilder("message", NamespaceURIs.JABBER_SERVER);
        String receiver = "test@example.com";
        stanzaBuilder.addAttribute("to", receiver);
        stanzaBuilder.startInnerElement("timestamp").addAttribute("value", timestamp).endInnerElement();

        sessionContext.setServerToServer();
        
        RecordingStanzaRelay stanzaRelay = (RecordingStanzaRelay) sessionContext.getServerRuntimeContext().getStanzaRelay();
        assertFalse(stanzaRelay.iterator().hasNext()); // nothing there yet

        Stanza stanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer responseStanzaContainer = messageHandler.execute(stanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);

        RecordingStanzaRelay.Triple triple = stanzaRelay.iterator().next();
        XMLElementVerifier timestampVerifier = triple.getStanza().getFirstInnerElement().getVerifier();
        
        assertTrue("stanza relayed to", triple.getStanza().getVerifier().toAttributeEquals(receiver));
        assertTrue("stanza relayed inner", timestampVerifier.attributeEquals("value", timestamp));
        assertEquals("stanza relayed to correct receiver", new EntityImpl("test", "example.com", null), triple.getEntity());

        // clean and do not accept follow-up relays
        stanzaRelay.reset();
        stanzaRelay.setAcceptingMode(false);
        
        responseStanzaContainer = messageHandler.execute(stanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);
        assertFalse(stanzaRelay.iterator().hasNext());
       
    }

    public void testStanzaReceiverUnavailable() throws EntityFormatException, DeliveryException {
        Entity sender = EntityImpl.parse("from@example.com");
        Entity receiver = EntityImpl.parse("to_exist@example.com");
        Entity noReceiver = EntityImpl.parse("to_unavail@example.com");

        StanzaReceiverRelay stanzaRelay = (StanzaReceiverRelay) sessionContext.getServerRuntimeContext().getStanzaRelay();
        StanzaReceiverQueue senderQueue = new StanzaReceiverQueue();
        StanzaReceiverQueue receiverQueue = new StanzaReceiverQueue();
        stanzaRelay.add(sender, senderQueue);
        stanzaRelay.add(receiver, receiverQueue);

        Stanza successfulMessageStanza = StanzaBuilder.createMessageStanza(sender, receiver, "en", "info").getFinalStanza();
        ResponseStanzaContainer responseStanzaContainer = messageHandler.execute(successfulMessageStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
        assertEquals(successfulMessageStanza, receiverQueue.getNext());

        Stanza failureMessageStanza = StanzaBuilder.createMessageStanza(sender, noReceiver, "en", "info").getFinalStanza();
        responseStanzaContainer = messageHandler.execute(failureMessageStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, sessionStateHolder);
        assertNull(receiverQueue.getNext());
        Stanza rejectionStanza = senderQueue.getNext();
        assertNotNull(rejectionStanza);
        XMPPCoreStanza rejectionCoreStanza = XMPPCoreStanza.getWrapper(rejectionStanza);
        assertEquals("error", rejectionCoreStanza.getType());

    }
    
}
