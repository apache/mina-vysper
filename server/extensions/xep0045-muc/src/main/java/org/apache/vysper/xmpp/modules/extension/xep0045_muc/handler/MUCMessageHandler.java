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
import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
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
        return MUCHandlerHelper.verifyNamespace(stanza);
    }
    
    private Stanza createMessageErrorStanza(Entity from, Entity to, String id, StanzaErrorType type, 
            StanzaErrorCondition errorCondition, Stanza stanza) {
        return MUCHandlerHelper.createErrorStanza("message", from, to, id, type.value(), errorCondition.value(), stanza.getInnerElements());
    }
    
    @Override
    protected Stanza executeMessageLogic(MessageStanza stanza,
            ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {

        logger.debug("Received message for MUC");
        Entity from = sessionContext.getInitiatingEntity();
        Entity roomWithNickJid = stanza.getTo();
        Entity roomJid = roomWithNickJid.getBareJID();

        MessageStanzaType type = stanza.getMessageType();
        if(type != null && type == MessageStanzaType.GROUPCHAT) {
            // groupchat, message to a room

            // must not have a nick
            if(roomWithNickJid.getResource() != null) {
                return createMessageErrorStanza(roomJid, from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.BAD_REQUEST, stanza);
            }
            
            logger.debug("Received groupchat message to {}", roomJid);
            Room room = conference.findRoom(roomJid);
            if(room != null) {
                Occupant sendingOccupant = room.findOccupantByJID(from);
                
                // sender must be participant in room
                if(sendingOccupant != null) {
                    
                    Entity roomAndSendingNick = new EntityImpl(room.getJID(), sendingOccupant.getName());
                    if(sendingOccupant.hasVoice()) {
                        // relay message to all occupants in room
                        
                        logger.debug("Relaying message to all room occupants");
                        for(Occupant occupent : room.getOccupants()) {
                            logger.debug("Relaying message to  {}", occupent);
                            List<Attribute> replaceAttributes = new ArrayList<Attribute>();
                            replaceAttributes.add(new Attribute("from", roomAndSendingNick.getFullQualifiedName()));
                            replaceAttributes.add(new Attribute("to", occupent.getJid().getFullQualifiedName()));
                            
                            relayStanza(occupent.getJid(), 
                                    StanzaBuilder.createClone(stanza, true, replaceAttributes).getFinalStanza(),
                                    sessionContext);
                        }
                    } else {
                        return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.FORBIDDEN, stanza);
                    }
                } else {
                    return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.NOT_ACCEPTABLE, stanza);
                }
            } else {
                return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
            }
        } else if(type == null  || type == MessageStanzaType.CHAT) {
            // private message
            logger.debug("Received direct message to {}", roomWithNickJid);
            Room room = conference.findRoom(roomJid);
            if(room != null) {
                Occupant sendingOccupant = room.findOccupantByJID(from);
                
                // sender must be participant in room
                if(sendingOccupant != null) {
                    Occupant receivingOccupant = room.findOccupantByNick(roomWithNickJid.getResource());
                    
                    // must be sent to an existing occupant in the room
                    if(receivingOccupant != null) {
                    
                        Entity roomAndSendingNick = new EntityImpl(room.getJID(), sendingOccupant.getName());
                        logger.debug("Relaying message to  {}", receivingOccupant);
                        List<Attribute> replaceAttributes = new ArrayList<Attribute>();
                        replaceAttributes.add(new Attribute("from", roomAndSendingNick.getFullQualifiedName()));
                        replaceAttributes.add(new Attribute("to", receivingOccupant.getJid().getFullQualifiedName()));
                    
                        relayStanza(receivingOccupant.getJid(), 
                            StanzaBuilder.createClone(stanza, true, replaceAttributes).getFinalStanza(),
                            sessionContext);
                    } else {
                        // TODO correct error?
                        return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
                    }
                } else {
                    return createMessageErrorStanza(room.getJID(), from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.NOT_ACCEPTABLE, stanza);
                }
            } else {
                return createMessageErrorStanza(moduleDomain, from, stanza.getID(), StanzaErrorType.MODIFY, StanzaErrorCondition.ITEM_NOT_FOUND, stanza);
            }
        }
        
        return null;
    }

    protected void relayStanza(Entity receiver, Stanza stanza,
            SessionContext sessionContext) {
        try {
            sessionContext.getServerRuntimeContext().getStanzaRelay().relay(
                    receiver, stanza, new IgnoreFailureStrategy());
        } catch (DeliveryException e) {
            logger.warn("presence relaying failed ", e);
        }
    }
}
