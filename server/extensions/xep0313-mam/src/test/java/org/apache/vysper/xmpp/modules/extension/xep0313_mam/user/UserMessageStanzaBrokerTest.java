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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.ServerRuntimeContextMock;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.SessionContextMock;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchivesMock;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchiveMock;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class UserMessageStanzaBrokerTest {

    private static final Entity JULIET_IN_CHAMBER = EntityImpl.parseUnchecked("juliet@capulet.lit/chamber");

    private static final Entity ROMEO_IN_ORCHARD = EntityImpl.parseUnchecked("romeo@capulet.lit/orchard");

    private static final Entity MACBETH_IN_KITCHEN = EntityImpl.parseUnchecked("macbeth@capulet.lit/kitchen");

    private static final Entity ALICE_IN_RABBIT_HOLE = EntityImpl.parseUnchecked("alice@capulet.lit/rabbit-hole");

    private static final Entity INITIATING_ENTITY = JULIET_IN_CHAMBER;

    private UserMessageArchiveMock julietArchive;

    private UserMessageArchiveMock romeoArchive;

    private UserMessageArchiveMock macbethArchive;

    private ServerRuntimeContextMock serverRuntimeContext;

    private SessionContextMock sessionContext;

    private StanzaBroker delegate = mock(StanzaBroker.class);

    @Before
    public void before() {
        serverRuntimeContext = new ServerRuntimeContextMock(EntityImpl.parseUnchecked("capulet.lit"));

        MessageArchivesMock archives = serverRuntimeContext.givenUserMessageArchives();

        julietArchive = archives.givenArchive(JULIET_IN_CHAMBER.getBareJID());
        romeoArchive = archives.givenArchive(ROMEO_IN_ORCHARD.getBareJID());
        macbethArchive = archives.givenArchive(MACBETH_IN_KITCHEN.getBareJID());

        sessionContext = new SessionContextMock();
        sessionContext.givenInitiatingEntity(INITIATING_ENTITY);
        sessionContext.givenServerRuntimeContext(serverRuntimeContext);
    }

    private UserMessageStanzaBroker buildTested(boolean isOutboundStanza, boolean archivingForced) {
        return new UserMessageStanzaBroker(delegate, serverRuntimeContext, sessionContext, isOutboundStanza,
                archivingForced);
    }

    private MessageStanza buildMessageStanza(MessageStanzaType messageStanzaType, Entity from, Entity to, String body) {
        return new MessageStanza(StanzaBuilder.createMessageStanza(from, to, messageStanzaType, "en", body).build());
    }

    @Test
    public void onlyNormalAndChatMessageAreArchived() {
        UserMessageStanzaBroker tested = buildTested(true, false);

        Stream.of(MessageStanzaType.values()).filter(messageStanzaType -> messageStanzaType != MessageStanzaType.NORMAL)
                .filter(messageStanzaType -> messageStanzaType != MessageStanzaType.CHAT).forEach(messageStanzaType -> {
                    MessageStanza stanza = buildMessageStanza(messageStanzaType, null, ALICE_IN_RABBIT_HOLE,
                            "hello world");

                    tested.writeToSession(stanza);

                    julietArchive.assertEmpty();
                    romeoArchive.assertEmpty();
                    macbethArchive.assertEmpty();
                });

        Stream.of(MessageStanzaType.CHAT, MessageStanzaType.NORMAL).forEach(messageStanzaType -> {
            julietArchive.clear();

            MessageStanza messageStanza = buildMessageStanza(messageStanzaType, null, ALICE_IN_RABBIT_HOLE,
                    "hello world");

            tested.writeToSession(messageStanza);

            julietArchive.assertUniqueArchivedMessageStanza(messageStanza);
        });
    }

    @Test
    public void outboundMessageHavingFrom() {
        UserMessageStanzaBroker tested = buildTested(true, false);
        MessageStanza messageStanza = buildMessageStanza(MessageStanzaType.NORMAL, ROMEO_IN_ORCHARD,
                ALICE_IN_RABBIT_HOLE, "hello world");

        tested.writeToSession(messageStanza);

        romeoArchive.assertUniqueArchivedMessageStanza(messageStanza);
    }

    @Test
    public void outboundMessageWithoutFrom() {
        UserMessageStanzaBroker tested = buildTested(true, false);

        MessageStanza messageStanza = buildMessageStanza(MessageStanzaType.NORMAL, null, ALICE_IN_RABBIT_HOLE,
                "hello world");

        tested.writeToSession(messageStanza);

        julietArchive.assertUniqueArchivedMessageStanza(messageStanza);
    }

    @Test
    public void unexistingArchive() {
        UserMessageStanzaBroker tested = buildTested(true, false);

        MessageStanza messageStanza = buildMessageStanza(MessageStanzaType.NORMAL, ALICE_IN_RABBIT_HOLE,
                ALICE_IN_RABBIT_HOLE, "hello world");

        tested.writeToSession(messageStanza);

        julietArchive.assertEmpty();
        romeoArchive.assertEmpty();
        macbethArchive.assertEmpty();
    }

    @Test
    public void messageWithoutBody() {
        UserMessageStanzaBroker tested = buildTested(true, false);
        MessageStanza messageStanza = buildMessageStanza(MessageStanzaType.NORMAL, JULIET_IN_CHAMBER, ROMEO_IN_ORCHARD,
                null);

        tested.writeToSession(messageStanza);

        julietArchive.assertEmpty();
        romeoArchive.assertEmpty();
    }
    
    @Test
    public void messageWithoutBodyWithArchivingForced(){
        UserMessageStanzaBroker tested = buildTested(true, true);
        MessageStanza messageStanza = buildMessageStanza(MessageStanzaType.NORMAL, JULIET_IN_CHAMBER, ROMEO_IN_ORCHARD,
                null);

        tested.writeToSession(messageStanza);

        julietArchive.assertUniqueArchivedMessageStanza(messageStanza);
        romeoArchive.assertEmpty();
    }

}