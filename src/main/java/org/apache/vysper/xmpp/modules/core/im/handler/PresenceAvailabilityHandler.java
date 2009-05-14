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
package org.apache.vysper.xmpp.modules.core.im.handler;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterUtils;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.*;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanzaVerifier;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * handling presence stanzas related to availability
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PresenceAvailabilityHandler extends AbstractPresenceSpecializedHandler {

    final Logger logger = LoggerFactory.getLogger(PresenceAvailabilityHandler.class);

    @Override
    /*package*/ Stanza executeCorePresence(ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza, SessionContext sessionContext, PresenceStanza presenceStanza, RosterManager rosterManager) {

        if (isSubscriptionType(presenceStanza.getPresenceType())) {
            throw new RuntimeException("case not handled in availability handler" + presenceStanza.getPresenceType().value());
        }

        // TODO: either use the resource associated with the session
        // (initiatingEntity)
        // or in case of multiple resources, use the from attribute or return an
        // error if the from attribute is not present.
        Entity initiatingEntity = sessionContext == null ? null : sessionContext.getInitiatingEntity();
        XMPPCoreStanzaVerifier verifier = presenceStanza.getCoreVerifier();
        ResourceRegistry registry = serverRuntimeContext.getResourceRegistry();

        // check if presence reception is turned of either globally or locally
        if (!serverRuntimeContext.getServerFeatures().isRelayingPresence() ||
            (sessionContext != null && sessionContext.getAttribute(SessionContext.SESSION_ATTRIBUTE_PRESENCE_STANZA_NO_RECEIVE) != null)) {
            return null;
        }

        PresenceStanzaType type = presenceStanza.getPresenceType();
        boolean available = PresenceStanzaType.isAvailable(type);

        if (isOutboundStanza) {
            Entity user = XMPPCoreStanzaHandler.determineFrom(presenceStanza, sessionContext);
            if (user == null) {
                return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER, presenceStanza, StanzaErrorType.MODIFY, "sender info insufficient: " + ((user == null) ? "no from" : user.getFullQualifiedName()), null, null);
            }

            if (available) {
                return handleOutboundAvailable(presenceStanza, serverRuntimeContext, sessionContext, rosterManager, user, registry);
            } else if (type == UNAVAILABLE) {
                return handleOutboundUnavailable(presenceStanza, serverRuntimeContext, sessionContext, rosterManager, user, registry);
            } else if (type == PROBE) {
                return handleOutboundPresenceProbe(presenceStanza, serverRuntimeContext, sessionContext, registry);
            } else if (type == ERROR) {
                throw new RuntimeException("not implemented yet");
            } else {
                throw new RuntimeException("unhandled outbound presence case " + type.value());
            }
        } else /* inbound */ {
            if (available) {
                return handleInboundAvailable(presenceStanza, serverRuntimeContext, sessionContext, registry);
            } else if (type == UNAVAILABLE) {
                return handleInboundUnavailable(presenceStanza, serverRuntimeContext, sessionContext, registry);
            } else if (type == PROBE) {
                return handleInboundPresenceProbe(presenceStanza, serverRuntimeContext, sessionContext, registry, rosterManager);
            } else if (type == ERROR) {
                throw new RuntimeException("not implemented yet");
            } else {
                throw new RuntimeException("unhandled inbound presence case " + type.value());
            }
        }
    }

    @SpecCompliance(compliant = {
        @SpecCompliant(spec = "RFC3921bis-08", section = "4.5.2", status = IN_PROGRESS)
    })
    private Stanza handleOutboundUnavailable(PresenceStanza presenceStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, RosterManager rosterManager, Entity user, ResourceRegistry registry) {

        if (!user.isResourceSet()) throw new RuntimeException("resource id not available");
        registry.setResourceState(user.getResource(), ResourceState.UNAVAILABLE);

        sessionContext.getServerRuntimeContext().getPresenceCache().remove(user);

        // TODO check if we do have to do something about resource priority

        List<Entity> contacts = new ArrayList<Entity>();

        Map<SubscriptionType, List<RosterItem>> itemMap = RosterUtils.getRosterItemsByState(rosterManager, user);
        List<RosterItem> item_FROM = itemMap.get(SubscriptionType.FROM);
        List<RosterItem> item_TO = itemMap.get(SubscriptionType.TO);
        List<RosterItem> item_BOTH = itemMap.get(SubscriptionType.BOTH);

        // broadcast presence from full JID to contacts
        // in roster with 'subscription' either 'from' or 'both'
        // TODO (for pres updates): ...and last presence stanza received from the contact during the user's
        // presence session was not of type "error" or "unsubscribe".

        List<RosterItem> rosterContacts_FROM = new ArrayList<RosterItem>();
        rosterContacts_FROM.addAll(item_FROM);
        rosterContacts_FROM.addAll(item_BOTH);
        for (RosterItem rosterContact : rosterContacts_FROM) {
            contacts.add(rosterContact.getJid());
        }

        // broadcast presence notification to all resources of
        // current entity.
        List<String> resources = registry.getAvailableResources(user);
        for (String resource : resources) {
            Entity otherResource = new EntityImpl(user, resource);
            contacts.add(otherResource);
        }

        // and send them out
        relayTo(user, contacts, presenceStanza, sessionContext);

        return null;
    }

    private Stanza handleOutboundPresenceProbe(PresenceStanza presenceStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, ResourceRegistry registry) {
        // outbound presence probes are against the spec.

        // TODO return error stanza
        throw new IllegalStateException("clients might not send presence probes");
    }

    private PresenceStanza handleOutboundAvailable(PresenceStanza presenceStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, RosterManager rosterManager, Entity user, ResourceRegistry registry) {
        boolean hasTo = presenceStanza.getCoreVerifier().attributePresent("to");
        if (hasTo) throw new RuntimeException("unhandled presence available case");

        if (!user.isResourceSet()) throw new RuntimeException("resource id not available");
        ResourceState resourceState = registry.getResourceState(user.getResource());

        boolean isPresenceUpdate = resourceState != null && ResourceState.isAvailable(resourceState);

        // TODO in case of !isPresenceUpdate, should we check for resourceState != ResourceState.AVAILABLE_INTERESTED ?
        // RFC3921bis-04#4.2.2 Initial Presence
        // RFC3921bis-04#4.4.2 Initial Presence

        updateLatestPresence(sessionContext, user, presenceStanza);
        if (!isPresenceUpdate) {
            // things to be done for initial presence

            // set resource state
            ResourceState currentState = registry.getResourceState(user.getResource());
            // set to AVAILABLE, but do not override AVAILABLE_INTERESTED
            registry.setResourceState(user.getResource(), ResourceState.makeAvailable(currentState));
        }
        updateResourcePriority(registry, sessionContext.getInitiatingEntity(), presenceStanza.getPrioritySafe());

        List<Entity> contacts = new ArrayList<Entity>();

        Map<SubscriptionType, List<RosterItem>> itemMap = RosterUtils.getRosterItemsByState(rosterManager, user);
        List<RosterItem> item_FROM = itemMap.get(SubscriptionType.FROM);
        List<RosterItem> item_TO = itemMap.get(SubscriptionType.TO);
        List<RosterItem> item_BOTH = itemMap.get(SubscriptionType.BOTH);

        // broadcast presence from full JID to contacts
        // in roster with 'subscription' either 'from' or 'both'
        // TODO: ...and user is not blocking outbound presence notifications above
        // TODO (for pres updates): ...and last presence stanza received from the contact during the user's
        // presence session was not of type "error" or "unsubscribe".

        List<RosterItem> rosterContacts_FROM = new ArrayList<RosterItem>();
        rosterContacts_FROM.addAll(item_FROM);
        rosterContacts_FROM.addAll(item_BOTH);
        for (RosterItem rosterContact : rosterContacts_FROM) {
            contacts.add(rosterContact.getJid());
        }

        // broadcast presence notification to all resources of
        // current entity.
        List<String> resources = registry.getAvailableResources(user);
        for (String resource : resources) {
            Entity otherResource = new EntityImpl(user, resource);
            contacts.add(otherResource);
        }

        // and send them out
        relayTo(user, contacts, presenceStanza, sessionContext);

        if (!isPresenceUpdate) {
            // initial presence only:
            // send probes to all contacts of the current jid where
            // 'subscription' is either 'to' or 'both'
            // TODO: ...and jid is not blocking inbound presence notification
            List<RosterItem> rosterContacts_TO = new ArrayList<RosterItem>();
            rosterContacts_TO.addAll(item_TO);
            rosterContacts_TO.addAll(item_BOTH);
            for (RosterItem rosterItem : rosterContacts_TO) {
                Entity contact_TO = rosterItem.getJid();
                Stanza probeStanza = buildPresenceStanza(user, contact_TO, PresenceStanzaType.PROBE, null);
                relayStanza(contact_TO, probeStanza, sessionContext);
            }
        }

        return null;
    }

    private PresenceStanza handleInboundUnavailable(PresenceStanza presenceStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, ResourceRegistry registry) {
        String unavailableContact = "UNKNOWN";
        if (presenceStanza != null && presenceStanza.getFrom() != null) {
            unavailableContact = presenceStanza.getFrom().getFullQualifiedName();
        }
        logger.info("{} has become unavailable", unavailableContact);

        return presenceStanza;
    }

    @SpecCompliant(spec = "RFC3921bis-04", section = "4.3.2")
	private XMPPCoreStanza handleInboundPresenceProbe(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, ResourceRegistry registry, RosterManager rosterManager) {
		Entity contact = stanza.getFrom();
		Entity user = stanza.getTo();

        RosterItem contactItem;
        try {
            contactItem = rosterManager.getContact(user, contact.getBareJID());
        } catch (RosterException e) {
            contactItem = null;
        }
        if (contactItem == null || !contactItem.hasFrom()) {
            // not a contact, or not a _subscribed_ contact!
			relayStanza(contact, buildPresenceStanza(user, contact, UNSUBSCRIBED, null), sessionContext);
            return null;
		}

        if (user.getResource() == null) {
            // presence probes must happen on resource level!
            relayStanza(contact, buildPresenceStanza(user, contact, UNSUBSCRIBED, null), sessionContext);
            return null;
        }

        PresenceStanza presenceStanza = retrieveLatestPresence(sessionContext, user);
        if (presenceStanza == null) {
            // we have no current presence info
            relayStanza(contact, buildPresenceStanza(user, contact, UNAVAILABLE, null), sessionContext);
            return null;
        }

        // return current presence as probing result
        relayStanza(contact, buildPresenceStanza(user, contact, null, presenceStanza.getInnerElements()), sessionContext);

		return null;
	}

    private void updateLatestPresence(SessionContext sessionContext, Entity user, PresenceStanza stanza) {
        sessionContext.getServerRuntimeContext().getPresenceCache().put(user, stanza);
    }

    private PresenceStanza retrieveLatestPresence(SessionContext sessionContext, Entity user) {
        return sessionContext.getServerRuntimeContext().getPresenceCache().get(user);
    }

    @SpecCompliant(spec = "RFC3921bis-04", section = "4.2.3")
	private PresenceStanza handleInboundAvailable(PresenceStanza stanza,
                                                     ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, ResourceRegistry registry) {

        // TODO ?check if user has blocked contact?

        // write inbound stanza to the user
        sessionContext.getResponseWriter().write(stanza);

        logger.info("{} has become available", stanza.getFrom().getFullQualifiedName());

        return stanza;
	}

    /**
     * the presence priority is optional, but if contained, it might become relevant for
     * message delivery (see RFC3921bis-05#8.3.1.1)
     */
    private void updateResourcePriority(ResourceRegistry registry, Entity initiatingEntity, int priority) {
        if (initiatingEntity == null || initiatingEntity.getResource() == null) return;
        registry.setResourcePriority(initiatingEntity.getResource(), priority);
    }

    private void relayTo(Entity from, List<Entity> tos, PresenceStanza original, SessionContext sessionContext) {
        List<Attribute> toFromReplacements = new ArrayList<Attribute>();
        toFromReplacements.add(new Attribute("from", from.getFullQualifiedName()));

        for (Entity to : tos) {
            toFromReplacements.add(new Attribute("to", to.getFullQualifiedName()));
            Stanza outgoingStanza = StanzaBuilder.createClone(original, true, toFromReplacements).getFinalStanza();
            relayStanza(to, outgoingStanza, sessionContext);
            toFromReplacements.remove(toFromReplacements.size()-1); // clear space for new 'to' attribute
        }
    }

}
