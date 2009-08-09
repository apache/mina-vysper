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
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec="xep-0045", section="7.1", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class MUCEnterRoomHandler extends DefaultPresenceHandler {

    final Logger logger = LoggerFactory.getLogger(MUCEnterRoomHandler.class);

    private Conference conference;
    
    public MUCEnterRoomHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC);
    }

    private Stanza sendError(Entity roomJid, Entity occupantJid, String error) {
        //        <presence
        //        from='darkcave@chat.shakespeare.lit'
        //        to='hag66@shakespeare.lit/pda'
        //        type='error'>
        //      <error type='modify'>
        //        <jid-malformed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
        //      </error>
        //    </presence>

        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomJid, occupantJid, null, 
                PresenceStanzaType.ERROR, null, null);
        
        // "Note: If an error occurs in relation to joining a room, the service SHOULD include 
        // the MUC child element (i.e., <x xmlns='http://jabber.org/protocol/muc'/>) in the 
        // <presence/> stanza of type "error"."
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC).endInnerElement();
        builder.startInnerElement("error").addAttribute("type", "modify");
        builder.startInnerElement(error, NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
        builder.endInnerElement();
        
        return PresenceStanza.getWrapper(builder.getFinalStanza());
    }
    
    @Override
    protected Stanza executePresenceLogic(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        // TODO handle null
        Entity roomAndNick = stanza.getTo();
        
        Entity roomJid = roomAndNick.getBareJID();
        String nick = roomAndNick.getResource();
        
        // TODO handle null
        Entity newOccupantJid = stanza.getFrom();
        
        // user did not send nick name
        if(nick == null) {
            return sendError(roomJid, newOccupantJid, "jid-malformed");
        }
        
        // TODO what to use for the room name?
        Room room = conference.findOrCreateRoom(roomJid, roomJid.getNode());
        
        Occupant newOccupant = room.addOccupant(newOccupantJid, nick);
        
        // relay presence of all existing room occupants to the now joined occupant
        for(Occupant occupant : room.getOccupants()) {
            sendExistingOccupantToNewOccupant(newOccupant, occupant, room, sessionContext);
        }
        
        // relay presence of the newly added occupant to all existing occupants
        for(Occupant occupant : room.getOccupants()) {
            sendNewOccupantPresence(newOccupant, occupant, room, sessionContext);
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
    
    private void sendNewOccupantPresence(Occupant newOccupant, Occupant existingOccupant, Room room, SessionContext sessionContext) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), newOccupant.getName());
        
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(roomAndNewUserNick, existingOccupant.getJid(), null, null, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC_USER);
        builder.startInnerElement("item")
            .addAttribute("affiliation", newOccupant.getAffiliation().toString())
            .addAttribute("role", newOccupant.getRole().toString())
            .endInnerElement();
        
        if(existingOccupant.getJid().equals(newOccupant.getJid())) {
            // send status to indicate that this is the users own presence
            builder.startInnerElement("status").addAttribute("code", "110").endInnerElement();
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
