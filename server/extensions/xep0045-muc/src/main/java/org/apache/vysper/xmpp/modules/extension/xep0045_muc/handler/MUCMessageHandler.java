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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.dataforms.VoiceRequestForm;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045
 * Multi-user chat</a>.
 * 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliance(compliant = {
        @SpecCompliant(spec = "xep-0045", section = "7.9", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL),
        @SpecCompliant(spec = "xep-0045", section = "7.9", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL) })
public class MUCMessageHandler extends DefaultMessageHandler {

    final Logger logger = LoggerFactory.getLogger(MUCMessageHandler.class);

    private Conference conference;

    private Entity moduleDomain;

    public MUCMessageHandler(Conference conference, Entity moduleDomain) {
        this.conference = conference;
        this.moduleDomain = moduleDomain;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        // accept all messages sent to this module
        return true;
    }

    private List<Stanza> createMessageErrorStanza(Entity from, Entity to, String id, StanzaErrorType type,
            StanzaErrorCondition errorCondition, Stanza stanza) {
        return Collections.singletonList(MUCHandlerHelper.createErrorStanza("message", NamespaceURIs.JABBER_CLIENT,
                from, to, id, type.value(), errorCondition.value(), stanza.getInnerElements()));
    }

    @Override
    protected List<Stanza> executeMessageLogic(MessageStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {

        logger.debug("Received message for MUC");
        Entity from = stanza.getFrom();
        Entity roomWithNickJid = stanza.getTo();
        Entity roomJid = roomWithNickJid.getBareJID();

        MessageStanzaType type = stanza.getMessageType();
        if (type == MessageStanzaType.GROUPCHAT) {
            // groupchat, message to a room

            // must not have a nick
            if (roomWithNickJid.getResource() != null) {
                return createMessageErrorStanza(roomJid, from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.BAD_REQUEST, stanza);
            }

            logger.debug("Received groupchat message to {}", roomJid);
            Room room = conference.findRoom(roomJid);
            if (room == null) {
                return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
            }

            Occupant sendingOccupant = room.findOccupantByJID(from);

            // sender must be participant in room
            if (sendingOccupant == null) {
                return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.NOT_ACCEPTABLE, stanza);
            }

            Entity roomAndSendingNick = new EntityImpl(room.getJID(), sendingOccupant.getNick());
            if (!sendingOccupant.hasVoice()) {
                return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.FORBIDDEN, stanza);
            }

            // relay message to all occupants in room
            try {
                if (stanza.getSubjects() != null && !stanza.getSubjects().isEmpty()) {
                    // subject message
                    if (!room.isRoomType(RoomType.OpenSubject) && !sendingOccupant.isModerator()) {
                        // room only allows moderators to change the subject, and sender is not a
                        // moderator
                        return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.AUTH,
                                StanzaErrorCondition.FORBIDDEN, stanza);
                    }
                }
            } catch (XMLSemanticError e) {
                // not a subject message, ignore exception
            }

            logger.debug("Relaying message to all room occupants");
            for (Occupant occupent : room.getOccupants()) {
                logger.debug("Relaying message to  {}", occupent);
                List<Attribute> replaceAttributes = new ArrayList<Attribute>();
                replaceAttributes.add(new Attribute("from", roomAndSendingNick.getFullQualifiedName()));
                replaceAttributes.add(new Attribute("to", occupent.getJid().getFullQualifiedName()));

                relayStanza(occupent.getJid(), StanzaBuilder.createClone(stanza, true, replaceAttributes).build(),
                        stanzaBroker);

            }

            // add to discussion history
            room.getHistory().append(stanza, sendingOccupant);
            room.updateLastActivity();
        } else if (type == null || type == MessageStanzaType.CHAT || type == MessageStanzaType.NORMAL) {
            // private message
            logger.debug("Received direct message to {}", roomWithNickJid);
            Room room = conference.findRoom(roomJid);
            if (room == null) {
                return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
            }

            room.updateLastActivity();

            Occupant sendingOccupant = room.findOccupantByJID(from);

            // sender must be participant in room
            if (roomWithNickJid.equals(roomJid)) {
                // check x element

                if (stanza.getVerifier().onlySubelementEquals("x", NamespaceURIs.JABBER_X_DATA)) {
                    // voice requests
                    logger.debug("Received voice request for room {}", roomJid);

                    handleVoiceRequest(from, sendingOccupant, room, stanza, stanzaBroker);
                } else if (stanza.getVerifier().onlySubelementEquals("x", NamespaceURIs.XEP0045_MUC_USER)) {
                    // invites/declines
                    return handleInvites(stanza, from, sendingOccupant, room, stanzaBroker);
                }
            } else if (roomWithNickJid.isResourceSet()) {
                if (sendingOccupant == null) {
                    // user must be occupant to send direct message
                    return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                            StanzaErrorCondition.NOT_ACCEPTABLE, stanza);
                }

                // got resource, private message for occupant
                Occupant receivingOccupant = room.findOccupantByNick(roomWithNickJid.getResource());

                // must be sent to an existing occupant in the room
                if (receivingOccupant == null) {
                    // TODO correct error?
                    return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY,
                            StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
                }

                Entity roomAndSendingNick = new EntityImpl(room.getJID(), sendingOccupant.getNick());
                logger.debug("Relaying message to  {}", receivingOccupant);
                List<Attribute> replaceAttributes = new ArrayList<Attribute>();
                replaceAttributes.add(new Attribute("from", roomAndSendingNick.getFullQualifiedName()));
                replaceAttributes.add(new Attribute("to", receivingOccupant.getJid().getFullQualifiedName()));

                relayStanza(receivingOccupant.getJid(),
                        StanzaBuilder.createClone(stanza, true, replaceAttributes).build(), stanzaBroker);
            }
        }

        return Collections.emptyList();
    }

    private List<Stanza> handleInvites(MessageStanza stanza, Entity from, Occupant sendingOccupant, Room room,
            StanzaBroker stanzaBroker) {
        X x = X.fromStanza(stanza);
        if (x != null && x.getInvite() != null) {
            if (sendingOccupant != null) {
                // invite, forward modified invite
                try {
                    Stanza invite = MUCHandlerHelper.createInviteMessageStanza(stanza, room.getPassword());
                    relayStanza(invite.getTo(), invite, stanzaBroker);
                } catch (EntityFormatException e) {
                    // invalid format of invite element
                    return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                            StanzaErrorCondition.JID_MALFORMED, stanza);
                }
            } else {
                // user must be occupant to send invite
                return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.NOT_ACCEPTABLE, stanza);
            }
        } else if (x != null && x.getDecline() != null) {
            // invite, forward modified decline
            try {
                Stanza decline = MUCHandlerHelper.createDeclineMessageStanza(stanza);
                relayStanza(decline.getTo(), decline, stanzaBroker);
            } catch (EntityFormatException e) {
                // invalid format of invite element
                return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                        StanzaErrorCondition.JID_MALFORMED, stanza);
            }
        } else {
            return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY,
                    StanzaErrorCondition.UNEXPECTED_REQUEST, stanza);
        }

        return Collections.emptyList();
    }

    private void handleVoiceRequest(Entity from, Occupant sendingOccupant, Room room, Stanza stanza,
            StanzaBroker stanzaBroker) {
        List<XMLElement> dataXs = stanza.getInnerElementsNamed("x", NamespaceURIs.JABBER_X_DATA);
        XMLElement dataX = dataXs.get(0);

        // check if "request_allow" is set
        List<XMLElement> fields = dataX.getInnerElementsNamed("field", NamespaceURIs.JABBER_X_DATA);
        String requestAllow = getFieldValue(fields, "muc#request_allow");
        if ("true".equals(requestAllow)) {
            // submitted voice grant, only allowed by moderators
            if (sendingOccupant.isModerator()) {
                String requestNick = getFieldValue(fields, "muc#roomnick");
                Occupant requestor = room.findOccupantByNick(requestNick);
                requestor.setRole(Role.Participant);

                // notify remaining users that user got role updated
                MucUserItem presenceItem = new MucUserItem(requestor.getAffiliation(), requestor.getRole());
                for (Occupant occupant : room.getOccupants()) {
                    Stanza presenceToRemaining = MUCStanzaBuilder.createPresenceStanza(requestor.getJidInRoom(),
                            occupant.getJid(), null, NamespaceURIs.XEP0045_MUC_USER, presenceItem);

                    relayStanza(occupant.getJid(), presenceToRemaining, stanzaBroker);
                }
            }
        } else if (requestAllow == null) {
            // no request allow, treat as voice request
            VoiceRequestForm requestForm = new VoiceRequestForm(from, sendingOccupant.getNick());

            for (Occupant moderator : room.getModerators()) {
                Stanza request = StanzaBuilder.createMessageStanza(room.getJID(), moderator.getJid(), null, null)
                        .addPreparedElement(requestForm.createFormXML()).build();

                relayStanza(moderator.getJid(), request, stanzaBroker);
            }
        }
    }

    private String getFieldValue(List<XMLElement> fields, String var) {
        for (XMLElement field : fields) {
            if (var.equals(field.getAttributeValue("var"))) {
                try {
                    return field.getSingleInnerElementsNamed("value", NamespaceURIs.JABBER_X_DATA).getInnerText()
                            .getText();
                } catch (XMLSemanticError e) {
                    return null;
                }
            }
        }
        return null;

    }

    protected void relayStanza(Entity receiver, Stanza stanza, StanzaBroker stanzaBroker) {
        try {
            stanzaBroker.write(receiver, stanza, IgnoreFailureStrategy.INSTANCE);
        } catch (DeliveryException e) {
            logger.warn("presence relaying failed ", e);
        }
    }
}
