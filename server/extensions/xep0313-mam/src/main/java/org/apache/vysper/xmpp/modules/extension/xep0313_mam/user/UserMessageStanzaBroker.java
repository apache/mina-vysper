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

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.MessageStanzaWithId;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.SimpleMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchive;
import org.apache.vysper.xmpp.protocol.DelegatingStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Réda Housni Alaoui
 */
class UserMessageStanzaBroker extends DelegatingStanzaBroker {

    private static final Logger LOG = LoggerFactory.getLogger(UserMessageStanzaBroker.class);

    private final ServerRuntimeContext serverRuntimeContext;

    private final SessionContext sessionContext;

    private final boolean isOutbound;

    UserMessageStanzaBroker(StanzaBroker delegate, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, boolean isOutbound) {
        super(delegate);
        this.serverRuntimeContext = requireNonNull(serverRuntimeContext);
        this.sessionContext = sessionContext;
        this.isOutbound = isOutbound;
    }

    @Override
    public void write(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        super.write(receiver, archive(stanza), deliveryFailureStrategy);
    }

    @Override
    public void writeToSession(Stanza stanza) {
        super.writeToSession(archive(stanza));
    }

    private Stanza archive(Stanza stanza) {
        if (!MessageStanza.isOfType(stanza)) {
            return stanza;
        }

        MessageStanza messageStanza = new MessageStanza(stanza);
        MessageStanzaType messageStanzaType = messageStanza.getMessageType();
        if (messageStanzaType != MessageStanzaType.NORMAL && messageStanzaType != MessageStanzaType.CHAT) {
            // A server SHOULD include in a user archive all of the messages a user sends
            // or receives of type 'normal' or 'chat' that contain a <body> element.
            LOG.debug("Message {} is neither of type 'normal' or 'chat'. It will not be archived.", messageStanza);
            return messageStanza;
        }

        Map<String, XMLElement> bodies;
        try {
            bodies = messageStanza.getBodies();
        } catch (XMLSemanticError xmlSemanticError) {
            return messageStanza;
        }
        if (bodies.isEmpty()) {
            // A server SHOULD include in a user archive all of the messages a user sends
            // or receives of type 'normal' or 'chat' that contain a <body> element.
            return messageStanza;
        }

        // TODO Check preferences
        if (isOutbound) {
            addToSenderArchive(messageStanza, sessionContext);
            return messageStanza;
        } else {
            return addToReceiverArchive(messageStanza).map(MessageStanzaWithId::new).map(MessageStanzaWithId::toStanza)
                    .orElse(stanza);
        }
    }

    private void addToSenderArchive(MessageStanza messageStanza, SessionContext sessionContext) {
        // Servers that expose archive messages of sent/received messages on behalf of
        // local users MUST expose these archives to the user on the user's bare JID.
        Entity senderArchiveId = XMPPCoreStanzaHandler.extractSenderJID(messageStanza, sessionContext).getBareJID();
        Optional<UserMessageArchive> senderArchive = messageArchives().retrieveUserMessageArchive(senderArchiveId);
        if (!senderArchive.isPresent()) {
            LOG.debug("No archive returned for sender with bare JID '{}'", senderArchiveId);
            return;
        }
        senderArchive.get().archive(new SimpleMessage(messageStanza));
    }

    private Optional<ArchivedMessage> addToReceiverArchive(MessageStanza messageStanza) {
        // Servers that expose archive messages of sent/received messages on behalf of
        // local users MUST expose these archives to the user on the user's bare JID.
        Entity receiverArchiveId = requireNonNull(messageStanza.getTo(), "No 'to' found in " + messageStanza)
                .getBareJID();
        return messageArchives().retrieveUserMessageArchive(receiverArchiveId)
                .map(messageArchive -> messageArchive.archive(new SimpleMessage(messageStanza)));
    }

    private MessageArchives messageArchives() {
        return requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);
    }
}
