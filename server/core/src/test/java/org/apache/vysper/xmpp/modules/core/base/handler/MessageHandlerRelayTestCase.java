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
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.modules.core.TestUser;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

import junit.framework.TestCase;

/**
 */
public class MessageHandlerRelayTestCase extends TestCase {
    private MessageHandler messageHandler = new MessageHandler();

    private TestSessionContext senderSessionContext;

    // private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    protected Entity sender;

    protected TestUser senderUser;

    private TestSessionContext receiverSessionContext;

    // private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    protected Entity receiver;

    protected TestUser receiverUser;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // sender
        senderSessionContext = TestSessionContext.createWithStanzaReceiverRelay(new SessionStateHolder());
        sender = EntityImpl.parse("sender@vysper.org");
        senderSessionContext.setInitiatingEntity(sender);
        senderUser = TestUser.createForSession(senderSessionContext, sender);

        // receiver
        receiverSessionContext = TestSessionContext.createWithStanzaReceiverRelay(new SessionStateHolder(),
                senderSessionContext.getServerRuntimeContext(),
                (StanzaReceiverRelay) senderSessionContext.getStanzaRelay());
        receiver = EntityImpl.parse("receiver@vysper.org");
        receiverSessionContext.setInitiatingEntity(receiver);
        receiverUser = TestUser.createForSession(receiverSessionContext, receiver, false);
    }

    public void testStanzaRelayed() {
        String timestamp = "" + System.currentTimeMillis();

        StanzaBuilder stanzaBuilder = new StanzaBuilder("message", NamespaceURIs.JABBER_SERVER);
        // receiver@vysper.org, NOT receiver@vysper.org/resourceID
        stanzaBuilder.addAttribute("to", receiverUser.getEntity().getBareJID().getFullQualifiedName());
        stanzaBuilder.startInnerElement("timestamp", NamespaceURIs.JABBER_SERVER).addAttribute("value", timestamp)
                .endInnerElement();

        senderSessionContext.setClientToServer();

        assertNull(receiverUser.getNextStanza()); // nothing there yet
        assertNull(senderUser.getNextStanza()); // nothing there yet

        Stanza stanza = stanzaBuilder.build();
        messageHandler.execute(stanza, senderSessionContext.getServerRuntimeContext(), true, senderSessionContext, null,
                new DefaultStanzaBroker(senderSessionContext.getStanzaRelay(), senderSessionContext));

        Stanza receivedStanza = receiverUser.getNextStanza();
        XMLElementVerifier timestampVerifier = receivedStanza.getFirstInnerElement().getVerifier();

        assertTrue("stanza relayed to",
                receivedStanza.getVerifier().toAttributeEquals(receiverUser.getEntity().getFullQualifiedName()));
        assertTrue("stanza relayed inner", timestampVerifier.attributeEquals("value", timestamp));
        assertNotNull("from added", receivedStanza.getFrom());
        assertNotNull("from is full JID", receivedStanza.getFrom().equals(senderUser.getEntityFQ()));
        assertEquals("stanza relayed to correct receiver", receiverUser.getEntity(), receivedStanza.getTo());

    }

    public void testStanzaReceiverUnavailable() throws EntityFormatException, DeliveryException {
        Entity sender = EntityImpl.parse("from@example.com/resID");
        Entity receiver = EntityImpl.parse("to_exist@example.com");
        Entity noReceiver = EntityImpl.parse("to_unavail@example.com");

        StanzaReceiverRelay stanzaRelay = (StanzaReceiverRelay) senderSessionContext.getStanzaRelay();
        StanzaReceiverQueue senderQueue = new StanzaReceiverQueue();
        StanzaReceiverQueue receiverQueue = new StanzaReceiverQueue();
        stanzaRelay.add(sender, senderQueue);
        stanzaRelay.add(receiver, receiverQueue);

        Stanza successfulMessageStanza = StanzaBuilder.createMessageStanza(sender, receiver, "en", "info").build();
        messageHandler.execute(successfulMessageStanza, senderSessionContext.getServerRuntimeContext(), true,
                senderSessionContext, null, new DefaultStanzaBroker(stanzaRelay, senderSessionContext));
        assertEquals(successfulMessageStanza, receiverQueue.getNext());

        Stanza failureMessageStanza = StanzaBuilder.createMessageStanza(sender, noReceiver, "en", "info").build();
        messageHandler.execute(failureMessageStanza, senderSessionContext.getServerRuntimeContext(), true,
                senderSessionContext, null, new DefaultStanzaBroker(stanzaRelay, senderSessionContext));
        assertNull(receiverQueue.getNext());
        Stanza rejectionStanza = senderQueue.getNext();
        assertNotNull(rejectionStanza);
        XMPPCoreStanza rejectionCoreStanza = XMPPCoreStanza.getWrapper(rejectionStanza);
        assertEquals("error", rejectionCoreStanza.getType());

    }

}
