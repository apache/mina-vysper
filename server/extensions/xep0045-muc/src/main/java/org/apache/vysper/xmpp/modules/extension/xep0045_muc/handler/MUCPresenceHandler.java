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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec="xep-0045", section="7.1", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class MUCPresenceHandler extends DefaultPresenceHandler {

    final Logger logger = LoggerFactory.getLogger(MUCPresenceHandler.class);

    private Conference conference;
    
    public MUCPresenceHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return MUCHandlerHelper.verifyNamespace(stanza);
    }

    private Stanza createPresenceErrorStanza(Entity from, Entity to, String type, String errorName) {
        // "Note: If an error occurs in relation to joining a room, the service SHOULD include 
        // the MUC child element (i.e., <x xmlns='http://jabber.org/protocol/muc'/>) in the 
        // <presence/> stanza of type "error"."

        XMLElement xElement = new XMLElement("x", NamespaceURIs.XEP0045_MUC, (List<Attribute>)null, (List<XMLFragment>)null);
        return MUCHandlerHelper.createErrorStanza("presence", from, to, type, errorName, Arrays.asList(xElement));
    }
    
    @Override
    protected Stanza executePresenceLogic(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        // TODO handle null
        Entity roomAndNick = stanza.getTo();
        // TODO handle null
        Entity occupantJid = stanza.getFrom();
        
        Entity roomJid = roomAndNick.getBareJID();
        String nick = roomAndNick.getResource();
        
        // user did not send nick name
        if(nick == null) {
            return createPresenceErrorStanza(roomJid, occupantJid, "modify", "jid-malformed");
        }

        String type = stanza.getType();
        
        if(type == null) {
            return enterRoom(stanza, roomJid, occupantJid, nick, sessionContext);
        } else if(type.equals("unavailable")) {
            return exitRoom(stanza, roomJid, occupantJid, nick, sessionContext);
        } else {
            throw new RuntimeException("Presence type not handled by MUC module: " + type);
        }
        
    }

    private Stanza enterRoom(PresenceStanza stanza, Entity roomJid,
            Entity newOccupantJid, String nick, SessionContext sessionContext) {
        // TODO what to use for the room name?
        Room room = conference.findOrCreateRoom(roomJid, roomJid.getNode());
        
        // check password if password protected
        if(room.isRoomType(RoomType.PasswordProtected)) {
            // TODO room constructor for password
            
            String password = null;
            try {
                XMLElement xElement = stanza.getSingleInnerElementsNamed("x");
                if(xElement != null) {
                    XMLElement passwordElement = xElement.getSingleInnerElementsNamed("password");
                    if(passwordElement != null) {
                        password = passwordElement.getInnerText().getText();
                    }
                }
            } catch (XMLSemanticError e) {
                password = null;
            }
            
            if(password == null || !password.equals(room.getPassword())) {
                // password missing or not matching
                return createPresenceErrorStanza(roomJid, newOccupantJid, "auth", "not-authorized");
            }
        }
        
        Occupant newOccupant = room.addOccupant(newOccupantJid, nick);
        
        // relay presence of all existing room occupants to the now joined occupant
        for(Occupant occupant : room.getOccupants()) {
            sendExistingOccupantToNewOccupant(newOccupant, occupant, room, sessionContext);
        }
        
        // relay presence of the newly added occupant to all existing occupants
        for(Occupant occupant : room.getOccupants()) {
            sendNewOccupantPresenceToExisting(newOccupant, occupant, room, sessionContext);
        }
        
        return null;
    }
    
    private Stanza exitRoom(PresenceStanza stanza, Entity roomJid,
            Entity occupantJid, String nick, SessionContext sessionContext) {
        Room room = conference.findRoom(roomJid);
        
        // room must exist, or we do nothing
        if(room != null) {
            Occupant exitingOccupant = room.findOccupant(occupantJid);
            
            // user must by in room, or we do nothing
            if(exitingOccupant != null) {
                Set<Occupant> allOccupants = room.getOccupants(); 
                
                room.removeOccupant(occupantJid);

                String statusMessage = null;
                try {
                    XMLElement statusElement = stanza.getSingleInnerElementsNamed("status");
                    if(statusElement != null && statusElement.getInnerText() != null) {
                        statusMessage = statusElement.getInnerText().getText();
                    }
                } catch (XMLSemanticError e) {
                    // ignore, status element did not exist
                }
                
                // relay presence of the newly added occupant to all existing occupants
                for(Occupant occupant : allOccupants) {
                    sendExitRoomPresenceToExisting(exitingOccupant, occupant, room, statusMessage, sessionContext);
                }
                
                if(room.isRoomType(RoomType.Temporary) && room.isEmpty()) {
                    conference.deleteRoom(roomJid);                    
                }
            }
        }
        
        return null;
    }

    private void sendExistingOccupantToNewOccupant(Occupant newOccupant, Occupant existingOccupant, Room room, SessionContext sessionContext) {
        //            <presence
        //            from='darkcave@chat.shakespeare.lit/firstwitch'
        //            to='hag66@shakespeare.lit/pda'>
        //          <x xmlns='http://jabber.org/protocol/muc#user'>
        //            <item affiliation='owner' role='moderator'/>
        //          </x>
        //        </presence>
        
        // do not send own presence
        if(existingOccupant.getJid().equals(newOccupant.getJid())) {
            return;
        }
        
        Entity roomAndOccupantNick = new EntityImpl(room.getJID(), existingOccupant.getName());
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomAndOccupantNick, newOccupant.getJid(), null, null, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC_USER);
        builder.startInnerElement("item")
            .addAttribute("affiliation", existingOccupant.getAffiliation().toString())
            .addAttribute("role", existingOccupant.getRole().toString())
            .endInnerElement();
        builder.endInnerElement();
        
        relayStanza(newOccupant.getJid(), builder.getFinalStanza(), sessionContext);
    }
    
    private void sendNewOccupantPresenceToExisting(Occupant newOccupant, Occupant existingOccupant, Room room, SessionContext sessionContext) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), newOccupant.getName());
        
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomAndNewUserNick, existingOccupant.getJid(), null, null, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC_USER);
        builder.startInnerElement("item")
            .addAttribute("affiliation", newOccupant.getAffiliation().toString())
            .addAttribute("role", newOccupant.getRole().toString());
        
        // section 7.1.5
        if(room.getRoomTypes().contains(RoomType.NonAnonymous) ||
                (room.getRoomTypes().contains(RoomType.SemiAnonymous) && existingOccupant.getRole() == Role.Moderator)) {
            // room is non-anonymous or semi-anonmoys and the occupant a moderator, send full user JID
            builder.addAttribute("jid", newOccupant.getJid().getFullQualifiedName());
            // TODO unit test for semi + moderator
        }
            
        builder.endInnerElement();
        
        if(existingOccupant.getJid().equals(newOccupant.getJid())) {
            
            if(room.getRoomTypes().contains(RoomType.NonAnonymous)) {
                // notify the user that this is a non-anonymous room
                builder.startInnerElement("status").addAttribute("code", "100").endInnerElement();
            }
            
            // send status to indicate that this is the users own presence
            builder.startInnerElement("status").addAttribute("code", "110").endInnerElement();
        }
        builder.endInnerElement();

        relayStanza(existingOccupant.getJid(), builder.getFinalStanza(), sessionContext);
    }
    
    private void sendExitRoomPresenceToExisting(Occupant exitingOccupant, Occupant existingOccupant, Room room, 
            String statusMessage, SessionContext sessionContext) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), exitingOccupant.getName());
        
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomAndNewUserNick, existingOccupant.getJid(), null, 
                PresenceStanzaType.UNAVAILABLE, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC_USER);
        builder.startInnerElement("item")
            .addAttribute("affiliation", exitingOccupant.getAffiliation().toString())
            // must be none since the user is leaving
            .addAttribute("role", "none");
            
        builder.endInnerElement();
        
        // is this stanza to be sent to the exiting user himself?
        boolean ownStanza = existingOccupant.getJid().equals(exitingOccupant.getJid()); 
        
        if(ownStanza || statusMessage != null) {
            builder.startInnerElement("status");
            
            if(ownStanza) {
                // send status to indicate that this is the users own presence
                builder.addAttribute("code", "110");
            }
            if(statusMessage != null) {
                builder.addText(statusMessage);
            }
            
            builder.endInnerElement();
        }
        builder.endInnerElement();

        relayStanza(existingOccupant.getJid(), builder.getFinalStanza(), sessionContext);
    }
    
    protected void relayStanza(Entity receiver, Stanza stanza, SessionContext sessionContext) {
        try {
                sessionContext.getServerRuntimeContext().getStanzaRelay().relay(receiver, stanza, new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
                logger.warn("presence relaying failed ", e);
        }
}
}
