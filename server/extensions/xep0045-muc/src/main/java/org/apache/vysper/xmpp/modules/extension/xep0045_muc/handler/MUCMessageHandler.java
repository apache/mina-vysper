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

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
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

    public MUCMessageHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return MUCHandlerHelper.verifyNamespace(stanza);
    }
    
    private Stanza copyMessageStanza(Entity from, Entity to, Stanza original) {
        StanzaBuilder builder = new StanzaBuilder("message");
        builder.addAttribute("from", from.getFullQualifiedName());
        builder.addAttribute("to", to.getFullQualifiedName());
        if(original.getAttribute("type") != null) {
            builder.addAttribute("type", original.getAttributeValue("type"));
        }
        
        for(XMLElement innerElement : original.getInnerElements()) {
            builder.addPreparedElement(innerElement);
        }
        
        return builder.getFinalStanza();
    }
    
    private Stanza createMessageErrorStanza(Entity from, Entity to, String id, String type, String errorName, Stanza stanza) {
        return MUCHandlerHelper.createErrorStanza("message", from, to, id, type, errorName, stanza.getInnerElements());
    }
    
    @Override
    protected Stanza executeMessageLogic(MessageStanza stanza,
            ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {

        logger.debug("Received message for MUC");
        MessageStanzaType type = stanza.getMessageType();
        if(type != null && type == MessageStanzaType.GROUPCHAT) {
            // groupchat, message to a room
            
            Entity roomWithNickJid = stanza.getTo();
            logger.debug("Received groupchat message to {}", roomWithNickJid);
            Room room = conference.findRoom(roomWithNickJid.getBareJID());
            if(room != null) {
                // sender must be participant in room
                Entity from = stanza.getFrom();
                if(from == null) {
                    from = sessionContext.getInitiatingEntity();
                }
                Occupant sendingOccupant = room.findOccupantByJID(from);
                
                if(sendingOccupant != null) {
                    
                    Entity roomAndSendingNick = new EntityImpl(room.getJID(), sendingOccupant.getName());
                    if(sendingOccupant.hasVoice()) {
                        // relay message to all occupants in room
                        
                        logger.debug("Relaying message to all room occupants");
                        for(Occupant occupent : room.getOccupants()) {
                            logger.debug("Relaying message to  {}", occupent);
                            relayStanza(occupent.getJid(), 
                                    copyMessageStanza(roomAndSendingNick, occupent.getJid(), stanza), 
                                    sessionContext);
                        }
                    } else {
                        return createMessageErrorStanza(room.getJID(), from, stanza.getID(), "modify", "forbidden", stanza);
                    }
                } else {
                    return createMessageErrorStanza(room.getJID(), from, stanza.getID(), "modify", "not-acceptable", stanza);
                }
            } else {
                // TODO how to handle unknown room?
            }
        } else {
            // TODO handle non-groupchat messages
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
