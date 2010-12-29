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

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliations;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.ServerAdministrationService;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0045", section = "7.1", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class MUCPresenceHandler extends DefaultPresenceHandler {

    final Logger logger = LoggerFactory.getLogger(MUCPresenceHandler.class);

    private Conference conference;

    public MUCPresenceHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        // accept all messages sent to this module
        return true;
    }

    private Stanza createPresenceErrorStanza(Entity from, Entity to, String id, String type, String errorName) {
        // "Note: If an error occurs in relation to joining a room, the service SHOULD include 
        // the MUC child element (i.e., <x xmlns='http://jabber.org/protocol/muc'/>) in the 
        // <presence/> stanza of type "error"."

        return MUCHandlerHelper.createErrorStanza("presence", NamespaceURIs.JABBER_CLIENT, from, to, id, type,
                errorName, Arrays.asList((XMLElement) new X()));
    }

    @Override
    protected Stanza executePresenceLogic(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        // TODO handle null
        Entity roomAndNick = stanza.getTo();

        Entity occupantJid = stanza.getFrom();

        Entity roomJid = roomAndNick.getBareJID();
        String nick = roomAndNick.getResource();

        // user did not send nick name
        if (nick == null) {
            return createPresenceErrorStanza(roomJid, occupantJid, stanza.getID(), "modify", "jid-malformed");
        }

        String type = stanza.getType();
        if (type == null) {
            return available(stanza, roomJid, occupantJid, nick, serverRuntimeContext);
        } else if (type.equals("unavailable")) {
            return unavailable(stanza, roomJid, occupantJid, nick, serverRuntimeContext);
        } else {
            throw new RuntimeException("Presence type not handled by MUC module: " + type);
        }

    }

    private String getInnerElementText(XMLElement element, String childName) {
        try {
            XMLElement childElm = element.getSingleInnerElementsNamed(childName);
            if (childElm != null && childElm.getInnerText() != null) {
                return childElm.getInnerText().getText();
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            return null;
        }
    }

    private Stanza available(PresenceStanza stanza, Entity roomJid, Entity newOccupantJid, String nick,
            ServerRuntimeContext serverRuntimeContext) {

        boolean newRoom = false;
        // TODO what to use for the room name?
        Room room = conference.findRoom(roomJid);
        if(room == null) {
            room = conference.createRoom(roomJid, roomJid.getNode());
            newRoom = true;
        }

        if (room.isInRoom(newOccupantJid)) {
            // user is already in room, change nick
            logger.debug("{} has requested to change nick in room {}", newOccupantJid, roomJid);

            // occupant is already in room
            Occupant occupant = room.findOccupantByJID(newOccupantJid);
            if (nick.equals(occupant.getNick())) {
                // nick unchanged, change show and status
                for (Occupant receiver : room.getOccupants()) {
                    sendChangeShowStatus(occupant, receiver, room, getInnerElementText(stanza, "show"),
                            getInnerElementText(stanza, "status"), serverRuntimeContext);
                }
            } else {
                if (room.isInRoom(nick)) {
                    // user with this nick is already in room
                    return createPresenceErrorStanza(roomJid, newOccupantJid, stanza.getID(), "cancel", "conflict");
                }

                String oldNick = occupant.getNick();
                // update the nick
                occupant.setNick(nick);

                // send out unavailable presences to all existing occupants
                for (Occupant receiver : room.getOccupants()) {
                    sendChangeNickUnavailable(occupant, oldNick, receiver, room, serverRuntimeContext);
                }

                // send out available presences to all existing occupants
                for (Occupant receiver : room.getOccupants()) {
                    sendChangeNickAvailable(occupant, receiver, room, serverRuntimeContext);
                }

            }
        } else {
            logger.debug("{} has requested to enter room {}", newOccupantJid, roomJid);

            boolean nickConflict = room.isInRoom(nick);
            boolean nickRewritten = false;
            int counter = 1; // max conflicts, to avoid DoS attacks
            String rewrittenNick = null;
            while (nickConflict && counter < 100 && room.rewritesDuplicateNick()) {
                rewrittenNick = nick + "_" + counter;
                nickConflict = room.isInRoom(rewrittenNick);
                if (nickConflict) {
                    counter++;
                } else {
                    nick = rewrittenNick;
                    nickRewritten = true;
                }
            }
                
            if (nickConflict) {
                // user with this nick is already in room
                return createPresenceErrorStanza(roomJid, newOccupantJid, stanza.getID(), "cancel", "conflict");
            }

            // check password if password protected
            if (room.isRoomType(RoomType.PasswordProtected)) {
                X x = X.fromStanza(stanza);
                String password = null;
                if (x != null) {
                    password = x.getPasswordValue();
                }

                if (password == null || !password.equals(room.getPassword())) {
                    // password missing or not matching
                    return createPresenceErrorStanza(roomJid, newOccupantJid, stanza.getID(), "auth", "not-authorized");
                }
            }

            Occupant newOccupant;
            try {
                newOccupant = room.addOccupant(newOccupantJid, nick);
            } catch(RuntimeException e) {
                return createPresenceErrorStanza(roomJid, newOccupantJid, stanza.getID(), "auth", e.getMessage());
            }
            
            if(newRoom) {
                room.getAffiliations().add(newOccupantJid, Affiliation.Owner);
                newOccupant.setRole(Role.Moderator);
            }

            // if the new occupant is a server admin, he will be for the room, too
            final ServerAdministrationService adhocCommandsService = (ServerAdministrationService)serverRuntimeContext.getServerRuntimeContextService(ServerAdministrationService.SERVICE_ID);
            if (adhocCommandsService != null && adhocCommandsService.isAdmin(newOccupantJid.getBareJID())) {
                final Affiliations roomAffiliations = room.getAffiliations();
                // make new occupant an Admin, but do not downgrade from Owner
                // Admin affilitation implies Moderator role (see XEP-0045 5.1.2)
                if (roomAffiliations.getAffiliation(newOccupantJid) != Affiliation.Owner) {
                    roomAffiliations.add(newOccupantJid, Affiliation.Admin);
                    newOccupant.setRole(Role.Moderator);
                }
            }
            
            // relay presence of all existing room occupants to the now joined occupant
            for (Occupant occupant : room.getOccupants()) {
                sendExistingOccupantToNewOccupant(newOccupant, occupant, room, serverRuntimeContext);
            }

            // relay presence of the newly added occupant to all existing occupants
            for (Occupant occupant : room.getOccupants()) {
                sendNewOccupantPresenceToExisting(newOccupant, occupant, room, serverRuntimeContext, nickRewritten);
            }

            // send discussion history to user
            boolean includeJid = room.isRoomType(RoomType.NonAnonymous);
            List<Stanza> history = room.getHistory().createStanzas(newOccupant, includeJid, History.fromStanza(stanza));
            relayStanzas(newOccupantJid, history, serverRuntimeContext);

            logger.debug("{} successfully entered room {}", newOccupantJid, roomJid);
        }
        return null;
    }

    private Stanza unavailable(PresenceStanza stanza, Entity roomJid, Entity occupantJid, String nick,
            ServerRuntimeContext serverRuntimeContext) {
        Room room = conference.findRoom(roomJid);

        // room must exist, or we do nothing
        if (room != null) {
            Occupant exitingOccupant = room.findOccupantByJID(occupantJid);

            // user must by in room, or we do nothing
            if (exitingOccupant != null) {
                Collection<Occupant> allOccupants = room.getOccupants();

                room.removeOccupant(occupantJid);

                // TODO replace with use of X
                String statusMessage = null;
                try {
                    XMLElement statusElement = stanza.getSingleInnerElementsNamed("status");
                    if (statusElement != null && statusElement.getInnerText() != null) {
                        statusMessage = statusElement.getInnerText().getText();
                    }
                } catch (XMLSemanticError e) {
                    // ignore, status element did not exist
                }

                // relay presence of the newly added occupant to all existing occupants
                for (Occupant occupant : allOccupants) {
                    sendExitRoomPresenceToExisting(exitingOccupant, occupant, room, statusMessage, serverRuntimeContext);
                }

                if (room.isRoomType(RoomType.Temporary) && room.isEmpty()) {
                    conference.deleteRoom(roomJid);
                }
            }
        }

        return null;
    }

    private void sendExistingOccupantToNewOccupant(Occupant newOccupant, Occupant existingOccupant, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        //            <presence
        //            from='darkcave@chat.shakespeare.lit/firstwitch'
        //            to='hag66@shakespeare.lit/pda'>
        //          <x xmlns='http://jabber.org/protocol/muc#user'>
        //            <item affiliation='owner' role='moderator'/>
        //          </x>
        //        </presence>

        // do not send own presence
        if (existingOccupant.getJid().equals(newOccupant.getJid())) {
            return;
        }

        Entity roomAndOccupantNick = new EntityImpl(room.getJID(), existingOccupant.getNick());
        Stanza presenceToNewOccupant = MUCStanzaBuilder.createPresenceStanza(roomAndOccupantNick, newOccupant.getJid(),
                null, NamespaceURIs.XEP0045_MUC_USER, new MucUserItem(existingOccupant.getAffiliation(),
                        existingOccupant.getRole()));

        logger.debug("Room presence from {} sent to {}", newOccupant, roomAndOccupantNick);
        relayStanza(newOccupant.getJid(), presenceToNewOccupant, serverRuntimeContext);
    }

    private void sendNewOccupantPresenceToExisting(Occupant newOccupant, Occupant existingOccupant, Room room,
                                                   ServerRuntimeContext serverRuntimeContext, boolean nickRewritten) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), newOccupant.getNick());

        List<XMLElement> inner = new ArrayList<XMLElement>();

        // room is non-anonymous or semi-anonymous and the occupant a moderator, send full user JID
        boolean includeJid = room.isRoomType(RoomType.NonAnonymous)
                || (room.isRoomType(RoomType.SemiAnonymous) && existingOccupant.getRole() == Role.Moderator);
        inner.add(new MucUserItem(newOccupant, includeJid, false));

        if (existingOccupant.getJid().equals(newOccupant.getJid())) {

            if (room.isRoomType(RoomType.NonAnonymous)) {
                // notify the user that this is a non-anonymous room
                inner.add(new Status(StatusCode.ROOM_NON_ANONYMOUS));
            }

            // send status to indicate that this is the users own presence
            inner.add(new Status(StatusCode.OWN_PRESENCE));
            if (nickRewritten) inner.add(new Status(StatusCode.NICK_MODIFIED));
        }

        Stanza presenceToExisting = MUCStanzaBuilder.createPresenceStanza(roomAndNewUserNick,
                existingOccupant.getJid(), null, NamespaceURIs.XEP0045_MUC_USER, inner);

        logger.debug("Room presence from {} sent to {}", roomAndNewUserNick, existingOccupant);
        relayStanza(existingOccupant.getJid(), presenceToExisting, serverRuntimeContext);
    }

    private void sendChangeNickUnavailable(Occupant changer, String oldNick, Occupant receiver, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndOldNick = new EntityImpl(room.getJID(), oldNick);

        List<XMLElement> inner = new ArrayList<XMLElement>();

        boolean includeJid = includeJidInItem(room, receiver);
        inner.add(new MucUserItem(changer, includeJid, true));
        inner.add(new Status(StatusCode.NEW_NICK));

        if (receiver.getJid().equals(changer.getJid())) {
            // send status to indicate that this is the users own presence
            inner.add(new Status(StatusCode.OWN_PRESENCE));
        }
        Stanza presenceToReceiver = MUCStanzaBuilder.createPresenceStanza(roomAndOldNick, receiver.getJid(),
                PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);

        logger.debug("Room presence from {} sent to {}", roomAndOldNick, receiver);
        relayStanza(receiver.getJid(), presenceToReceiver, serverRuntimeContext);
    }

    private void sendChangeShowStatus(Occupant changer, Occupant receiver, Room room, String show, String status,
            ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndNick = new EntityImpl(room.getJID(), changer.getNick());

        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomAndNick, receiver.getJid(), null, null, show,
                status);

        boolean includeJid = includeJidInItem(room, receiver);
        //        if(receiver.getJid().equals(changer.getJid())) {
        //            // send status to indicate that this is the users own presence
        //            new Status(StatusCode.OWN_PRESENCE).insertElement(builder);
        //        }

        builder.addPreparedElement(new X(NamespaceURIs.XEP0045_MUC_USER, new MucUserItem(changer, includeJid,
                true)));

        logger.debug("Room presence from {} sent to {}", roomAndNick, receiver);
        relayStanza(receiver.getJid(), builder.build(), serverRuntimeContext);
    }

    private boolean includeJidInItem(Room room, Occupant receiver) {
        // room is non-anonymous or semi-anonymous and the occupant a moderator, send full user JID
        return room.isRoomType(RoomType.NonAnonymous)
                || (room.isRoomType(RoomType.SemiAnonymous) && receiver.getRole() == Role.Moderator);
    }

    private void sendChangeNickAvailable(Occupant changer, Occupant receiver, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndOldNick = new EntityImpl(room.getJID(), changer.getNick());

        List<XMLElement> inner = new ArrayList<XMLElement>();
        boolean includeJid = includeJidInItem(room, receiver);
        inner.add(new MucUserItem(changer, includeJid, false));

        if (receiver.getJid().equals(changer.getJid())) {
            // send status to indicate that this is the users own presence
            inner.add(new Status(StatusCode.OWN_PRESENCE));
        }
        Stanza presenceToReceiver = MUCStanzaBuilder.createPresenceStanza(roomAndOldNick, receiver.getJid(), null,
                NamespaceURIs.XEP0045_MUC_USER, inner);

        relayStanza(receiver.getJid(), presenceToReceiver, serverRuntimeContext);
    }

    private void sendExitRoomPresenceToExisting(Occupant exitingOccupant, Occupant existingOccupant, Room room,
            String statusMessage, ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), exitingOccupant.getNick());

        List<XMLElement> inner = new ArrayList<XMLElement>();
        inner.add(new MucUserItem(null, null, existingOccupant.getAffiliation(), Role.None));

        // is this stanza to be sent to the exiting user himself?
        boolean ownStanza = existingOccupant.getJid().equals(exitingOccupant.getJid());

        if (ownStanza || statusMessage != null) {

            Status status;
            if (ownStanza) {
                // send status to indicate that this is the users own presence
                status = new Status(StatusCode.OWN_PRESENCE, statusMessage);
            } else {
                status = new Status(statusMessage);
            }
            inner.add(status);
        }

        Stanza presenceToExisting = MUCStanzaBuilder.createPresenceStanza(roomAndNewUserNick,
                existingOccupant.getJid(), PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);

        relayStanza(existingOccupant.getJid(), presenceToExisting, serverRuntimeContext);
    }

    protected void relayStanzas(Entity receiver, List<Stanza> stanzas, ServerRuntimeContext serverRuntimeContext) {
        for (Stanza stanza : stanzas) {
            relayStanza(receiver, stanza, serverRuntimeContext);
        }
    }

    protected void relayStanza(Entity receiver, Stanza stanza, ServerRuntimeContext serverRuntimeContext) {
        try {
            serverRuntimeContext.getStanzaRelay().relay(receiver, stanza, new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
            logger.warn("presence relaying failed ", e);
        }
    }
}
