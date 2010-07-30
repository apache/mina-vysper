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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserPresenceItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCIqAdminHandler extends DefaultIQHandler {

    final Logger logger = LoggerFactory.getLogger(MUCIqAdminHandler.class);

    private Conference conference;

    public MUCIqAdminHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC_ADMIN);
    }

    private Entity roomAndNick(Room room, Occupant occupant) {
        return new EntityImpl(room.getJID(), occupant.getName());
    }

    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        Room room = conference.findRoom(stanza.getTo());

        Occupant moderator = room.findOccupantByJID(stanza.getFrom());

        // check if moderator
        if (moderator.getRole() != Role.Moderator) {
            // only moderators are allowed to continue
            return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.AUTH, StanzaErrorCondition.FORBIDDEN);
        }

        try {
            XMLElement query = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0045_MUC_ADMIN);
            XMLElement itemElement = query.getSingleInnerElementsNamed("item", NamespaceURIs.XEP0045_MUC_ADMIN);

            IqAdminItem item = IqAdminItem.getWrapper(itemElement);

            Occupant target = null;
            if (item.getNick() != null) {
                target = room.findOccupantByNick(item.getNick());
            } else {
                return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Missing nick for item");
            }

            if (item.getRole() != null) {
                return changeRole(stanza, serverRuntimeContext, sessionContext, item, room, moderator, target);
            } else {
                return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Unknown IQ stanza");
            }

        } catch (XMLSemanticError e) {
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext,
                    "iq stanza of type set requires exactly one query element");
        }

    }

    private Stanza createBadRequestError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, String message) {
        return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                StanzaErrorType.MODIFY, "iq stanza of type set requires exactly one query element",
                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }

    private Stanza changeRole(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, IqAdminItem item, Room room, Occupant moderator, Occupant target) {
        Role newRole = item.getRole();

        // you can not change yourself
        if (moderator.getJid().equals(target.getJid())) {
            return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL, StanzaErrorCondition.CONFLICT);
        }

        // verify change
        if (newRole == Role.None) {
            // a moderator can not kick someone with a higher affiliation
            if (target.getAffiliation().compareTo(moderator.getAffiliation()) < 0) {
                return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        } else if (newRole == Role.Visitor) {
            // moderator, admin and owner can not have their voice revoked
            if (target.getAffiliation() == Affiliation.Admin || target.getAffiliation() == Affiliation.Owner) {
                return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        } else if (newRole == Role.Participant) {
            if (target.getRole() == Role.Moderator) {
                // only admin and owner might revoke moderator
                if (moderator.getAffiliation() != Affiliation.Admin && moderator.getAffiliation() != Affiliation.Owner) {
                    return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL,
                            StanzaErrorCondition.NOT_ALLOWED);
                }
                // admin and owners can not be revoked
                if (target.getAffiliation() == Affiliation.Admin || target.getAffiliation() == Affiliation.Owner) {
                    return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL,
                            StanzaErrorCondition.NOT_ALLOWED);
                }
            }
        } else if (newRole == Role.Moderator) {
            // only admin and owner might grant moderator
            if (moderator.getAffiliation() != Affiliation.Admin && moderator.getAffiliation() != Affiliation.Owner) {
                return MUCHandlerHelper.createErrorReply(stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        }

        target.setRole(newRole);
        if (newRole == Role.None) {
            // remove user from room
            room.removeOccupant(target.getJid());
        }

        Entity targetInRoom = roomAndNick(room, target);

        Status status = null;
        if (newRole == Role.None) {
            status = new Status(StatusCode.BEEN_KICKED);

            // notify user he got kicked
            Stanza presenceToKicked = MUCStanzaBuilder.createPresenceStanza(targetInRoom, target.getJid(),
                    PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, new MucUserPresenceItem(
                            Affiliation.None, Role.None),
                    // TODO handle <actor>
                    // TODO handle <reason>
                    status);

            relayStanza(target.getJid(), presenceToKicked, serverRuntimeContext);
        }

        PresenceStanzaType availType = (newRole == Role.None) ? PresenceStanzaType.UNAVAILABLE : null;

        // notify remaining users that user got role updated
        MucUserPresenceItem presenceItem = new MucUserPresenceItem(target.getAffiliation(), newRole);
        for (Occupant occupant : room.getOccupants()) {
            Stanza presenceToRemaining = MUCStanzaBuilder.createPresenceStanza(targetInRoom, occupant.getJid(),
                    availType, NamespaceURIs.XEP0045_MUC_USER, presenceItem, status);

            relayStanza(occupant.getJid(), presenceToRemaining, serverRuntimeContext);
        }

        return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .build();
    }

    protected void relayStanza(Entity receiver, Stanza stanza, ServerRuntimeContext serverRuntimeContext) {
        try {
            serverRuntimeContext.getStanzaRelay().relay(receiver, stanza, new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
            logger.warn("presence relaying failed ", e);
        }
    }

}
