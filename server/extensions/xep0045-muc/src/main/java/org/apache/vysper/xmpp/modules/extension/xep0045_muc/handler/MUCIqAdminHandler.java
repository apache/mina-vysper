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

import java.util.List;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserPresenceItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
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
	protected Stanza handleSet(IQStanza stanza,
			ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {

		Room room = conference.findRoom(stanza.getTo());
		
		Occupant moderator = room.findOccupantByJID(stanza.getFrom());
		
		// TODO add check if moderator or admin
		
		try {
			List<IqAdminItem> items = IqAdminItem.extractItems(stanza);
			
			for(IqAdminItem item : items) {
				
				
				if(item.getRole().equals(Role.None)) {
					// kicking a user
					
					// find kicked users jid
					Occupant kicked = null;
					if(item.getNick() != null) {
						kicked = room.findOccupantByNick(item.getNick());
					} else {
						// TODO fix
					}
					
					// remove user from room
					kicked.setRole(Role.None);
					room.removeOccupant(kicked.getJid());
					Entity kickedInRoom = roomAndNick(room, kicked);
					
					// notify user he got kicked
					StanzaBuilder presenceBuilder = StanzaBuilder.createPresenceStanza(kickedInRoom, kicked.getJid(), null, 
							PresenceStanzaType.UNAVAILABLE, null, null);
					presenceBuilder.addPreparedElement(new X(NamespaceURIs.XEP0045_MUC_USER, 
							new MucUserPresenceItem(Affiliation.None, Role.None),
							// TODO handle <actor>
							// TODO handle <reason>
							new Status(StatusCode.BEEN_KICKED)));

					relayStanza(kicked.getJid(), presenceBuilder.build(), serverRuntimeContext);
					
					// notify remaining users that user got kicked
					for(Occupant remaining : room.getOccupants()) {
						StanzaBuilder presenceToRemainingBuilder = StanzaBuilder.createPresenceStanza(kickedInRoom, remaining.getJid(), null, 
								PresenceStanzaType.UNAVAILABLE, null, null);
						presenceToRemainingBuilder.addPreparedElement(new X(NamespaceURIs.XEP0045_MUC_USER, 
								new MucUserPresenceItem(Affiliation.None, Role.None),
								new Status(StatusCode.BEEN_KICKED)));
						
						relayStanza(remaining.getJid(), presenceToRemainingBuilder.build(), serverRuntimeContext);
					}
				}
			}
			
	        return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID()).build();
			
		} catch (XMLSemanticError e) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY,
                    "iq stanza of type set requires exactly one query element",
                    getErrorLanguage(serverRuntimeContext, sessionContext), null);
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
