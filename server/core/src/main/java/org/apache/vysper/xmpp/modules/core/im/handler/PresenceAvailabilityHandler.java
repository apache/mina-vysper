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

import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.ERROR;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.PROBE;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNAVAILABLE;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNSUBSCRIBED;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.isSubscriptionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.OfflineStorageProvider;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterUtils;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.commandstanza.EndOfSessionCommandStanza;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanzaVerifier;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handling presence stanzas related to availability
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PresenceAvailabilityHandler extends AbstractPresenceSpecializedHandler {

    protected static final String DIRECTED_PRESENCE_MAP = "DIRECTED_PRESENCE_MAP_";

    final Logger logger = LoggerFactory.getLogger(PresenceAvailabilityHandler.class);

    /**
     * handles availability presence stanzas. prepares further processing of the
     * stanza and decides which special case of availability to transfer to.
     */
    @Override
    /* package */Stanza executeCorePresence(ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, PresenceStanza presenceStanza, RosterManager rosterManager,
            StanzaBroker stanzaBroker) {

        // do not handle other cases of presence
        if (isSubscriptionType(presenceStanza.getPresenceType())) {
            throw new RuntimeException(
                    "case not handled in availability handler" + presenceStanza.getPresenceType().value());
        }

        // TODO: either use the resource associated with the session
        // (initiatingEntity)
        // or in case of multiple resources, use the from attribute or return an
        // error if the from attribute is not present.
        Entity initiatingEntity = sessionContext == null ? null : sessionContext.getInitiatingEntity();
        XMPPCoreStanzaVerifier verifier = presenceStanza.getCoreVerifier();
        ResourceRegistry registry = serverRuntimeContext.getResourceRegistry();

        // check if presence reception is turned off either globally or locally
        if (!serverRuntimeContext.getServerFeatures().isRelayingPresence() || (sessionContext != null
                && sessionContext.getAttribute(SessionContext.SESSION_ATTRIBUTE_PRESENCE_STANZA_NO_RECEIVE) != null)) {
            return null;
        }

        PresenceStanzaType type = presenceStanza.getPresenceType();
        boolean available = PresenceStanzaType.isAvailable(type);

        if (isOutboundStanza) {
            Entity user = XMPPCoreStanzaHandler.extractUniqueSenderJID(presenceStanza, sessionContext);
            if (user == null) {
                return ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER, presenceStanza,
                        StanzaErrorType.MODIFY, "sender info insufficient: no from", null, null);
            }

            if (available) {
                return handleOutboundAvailable(presenceStanza, serverRuntimeContext, sessionContext, rosterManager,
                        user, registry, stanzaBroker);
            } else if (type == UNAVAILABLE) {
                return handleOutboundUnavailable(presenceStanza, sessionContext, rosterManager, user, registry,
                        stanzaBroker);
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
                return handleInboundPresenceProbe(presenceStanza, sessionContext, rosterManager, stanzaBroker);
            } else if (type == ERROR) {
                return handleInboundPresenceError(presenceStanza, serverRuntimeContext, sessionContext, registry);
            } else {
                throw new RuntimeException("unhandled inbound presence case " + type.value());
            }
        }
    }

    private Stanza handleInboundPresenceError(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, ResourceRegistry registry) {
        return stanza; // send to client
    }

    @SpecCompliance(compliant = { @SpecCompliant(spec = "RFC3921bis-08", section = "4.5.2", status = IN_PROGRESS) })
    private Stanza handleOutboundUnavailable(PresenceStanza presenceStanza, SessionContext sessionContext,
            RosterManager rosterManager, Entity user, ResourceRegistry registry, StanzaBroker stanzaBroker) {

        boolean hasTo = presenceStanza.getCoreVerifier().attributePresent("to");
        if (hasTo)
            return handleOutboundDirectedPresence(presenceStanza, sessionContext, rosterManager, registry, true,
                    stanzaBroker);

        if (!user.isResourceSet())
            throw new RuntimeException("resource id not available");
        boolean stateChanged = registry.setResourceState(user.getResource(), ResourceState.UNAVAILABLE);
        // avoid races from closing connections and unavail presence stanza handlings
        // happening quasi-concurrently
        if (!stateChanged)
            return null;

        sessionContext.getServerRuntimeContext().getPresenceCache().remove(user);

        SessionContext.SessionTerminationCause terminationCause = null;
        if (presenceStanza instanceof EndOfSessionCommandStanza) {
            EndOfSessionCommandStanza commandStanza = (EndOfSessionCommandStanza) presenceStanza;
            terminationCause = commandStanza.getSessionTerminationCause();
        }

        // TODO check if we do have to do something about resource priority

        List<Entity> contacts = new ArrayList<Entity>();

        Map<SubscriptionType, List<RosterItem>> itemMap = RosterUtils.getRosterItemsByState(rosterManager, user);
        List<RosterItem> item_FROM = itemMap.get(SubscriptionType.FROM);
        List<RosterItem> item_TO = itemMap.get(SubscriptionType.TO);
        List<RosterItem> item_BOTH = itemMap.get(SubscriptionType.BOTH);

        // broadcast presence from full JID to contacts
        // in roster with 'subscription' either 'from' or 'both'
        // TODO (for pres updates): ...and last presence stanza received from the
        // contact during the user's
        // presence session was not of type "error" or "unsubscribe".

        List<RosterItem> rosterContacts_FROM = new ArrayList<RosterItem>();
        rosterContacts_FROM.addAll(item_FROM);
        rosterContacts_FROM.addAll(item_BOTH);
        for (RosterItem rosterContact : rosterContacts_FROM) {
            contacts.add(rosterContact.getJid());
        }

        // broadcast unavailable to all directed-presence contacts
        Set<Entity> entitySet = getDirectedPresenceMap(sessionContext, user);
        if (entitySet != null) {
            logger.debug("sending unavailable info to " + entitySet.size() + " directed presence contacts for " + user);
            contacts.addAll(entitySet);
            entitySet.clear(); // and un-record them
        }

        // broadcast presence notification to all resources of
        // current entity.
        List<String> resources = registry.getAvailableResources(user);
        if (!SessionContext.SessionTerminationCause.isClientReceivingStanzas(terminationCause)) {
            resources.remove(user.getResource());
        }
        for (String resource : resources) {
            Entity otherResource = new EntityImpl(user, resource);
            contacts.add(otherResource);
        }

        // and send them out
        relayTo(user, contacts, presenceStanza, stanzaBroker);

        return null;
    }

    @SpecCompliant(spec = "RFC3921bis-08", section = "4.3.1", status = IN_PROGRESS)
    private Stanza handleOutboundPresenceProbe(PresenceStanza presenceStanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, ResourceRegistry registry) {
        // outbound presence probes are against the spec.

        // TODO return error stanza
        throw new IllegalStateException("clients might not send presence probes");
    }

    @SpecCompliant(spec = "RFC3921bis-08", section = "4.2.2", status = IN_PROGRESS)
    private PresenceStanza handleOutboundAvailable(PresenceStanza presenceStanza,
            ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, RosterManager rosterManager,
            Entity user, ResourceRegistry registry, StanzaBroker stanzaBroker) {
        boolean hasTo = presenceStanza.getCoreVerifier().attributePresent("to");
        if (hasTo)
            return handleOutboundDirectedPresence(presenceStanza, sessionContext, rosterManager, registry, false,
                    stanzaBroker);

        if (!user.isResourceSet())
            throw new RuntimeException("resource id not available");
        String resourceId = user.getResource();
        ResourceState resourceState = registry.getResourceState(resourceId);

        boolean isPresenceUpdate = resourceState != null && ResourceState.isAvailable(resourceState);

        // TODO in case of !isPresenceUpdate, should we check for resourceState !=
        // ResourceState.AVAILABLE_INTERESTED ?
        // RFC3921bis-04#4.2.2 Initial Presence
        // RFC3921bis-04#4.4.2 Initial Presence

        updateLatestPresence(sessionContext, user, presenceStanza);
        if (!isPresenceUpdate) {
            // things to be done for initial presence

            // set resource state
            ResourceState currentState = registry.getResourceState(resourceId);
            // set to AVAILABLE, but do not override AVAILABLE_INTERESTED
            registry.setResourceState(resourceId, ResourceState.makeAvailable(currentState));
        }

        // the presence priority is optional, but if contained, it might become relevant
        // for
        // message delivery (see RFC3921bis-05#8.3.1.1)
        registry.setResourcePriority(resourceId, presenceStanza.getPrioritySafe());

        // check for pending offline stored stanzas, and send them out
        OfflineStorageProvider offlineProvider = serverRuntimeContext.getStorageProvider(OfflineStorageProvider.class);
        if (offlineProvider == null) {
            logger.warn("No Offline Storage Provider configured");
        } else {
            Collection<Stanza> offlineStanzas = offlineProvider.getStanzasFor(user);
            for (Stanza stanza : offlineStanzas) {
                logger.debug("Sending out delayed offline stanza");
                stanzaBroker.writeToSession(stanza);
            }
        }

        List<Entity> contacts = new ArrayList<Entity>();

        Map<SubscriptionType, List<RosterItem>> itemMap = RosterUtils.getRosterItemsByState(rosterManager, user);
        List<RosterItem> item_FROM = itemMap.get(SubscriptionType.FROM);
        List<RosterItem> item_TO = itemMap.get(SubscriptionType.TO);
        List<RosterItem> item_BOTH = itemMap.get(SubscriptionType.BOTH);

        // broadcast presence from full JID to contacts
        // in roster with 'subscription' either 'from' or 'both'
        // TODO: ...and user is not blocking outbound presence notifications above
        // TODO (for pres updates): ...and last presence stanza received from the
        // contact during the user's
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
        relayTo(user, contacts, presenceStanza, stanzaBroker);

        if (!isPresenceUpdate) {
            // initial presence only:
            // send probes to all contacts of the current jid where
            // 'subscription' is either 'to' or 'both'
            // TODO: ...and jid is not blocking inbound presence notification
            // TODO: optimize: don't send server-local probes when contact's presence is
            // known locally
            List<RosterItem> rosterContacts_TO = new ArrayList<RosterItem>();
            rosterContacts_TO.addAll(item_TO);
            rosterContacts_TO.addAll(item_BOTH);
            for (RosterItem rosterItem : rosterContacts_TO) {
                Entity contact_TO = rosterItem.getJid();
                Stanza probeStanza = buildPresenceStanza(user, contact_TO, PresenceStanzaType.PROBE, null);
                relayStanza(contact_TO, probeStanza, stanzaBroker);
            }
        }

        return null;
    }

    @SpecCompliant(spec = "RFC3921bis-08", section = "4.6.2")
    private PresenceStanza handleOutboundDirectedPresence(PresenceStanza presenceStanza, SessionContext sessionContext,
            RosterManager rosterManager, ResourceRegistry registry, final boolean unvailable,
            StanzaBroker stanzaBroker) {
        final Entity to = presenceStanza.getTo();
        Entity from = presenceStanza.getFrom();

        Stanza redirectDirectedStanza = presenceStanza;
        if (from == null || !from.isResourceSet()) {
            from = new EntityImpl(sessionContext.getInitiatingEntity(),
                    registry.getUniqueResourceForSession(sessionContext));
            redirectDirectedStanza = StanzaBuilder.createForwardStanza(presenceStanza, from, null);
        }

        Set<Entity> dpMap = getDirectedPresenceMap(sessionContext, from);

        boolean isFromContact;
        try {
            isFromContact = rosterManager.retrieve(from.getBareJID()).getEntry(to.getBareJID()).hasFrom();
        } catch (Exception e) {
            isFromContact = false;
        }
        boolean IsTOAvailable = !ResourceState.isAvailable(registry.getResourceState(from.getResource()));

        if (unvailable) {
            dpMap.remove(to);
            logger.debug("removed directed presence between " + from + " and " + to);
        } else {
            if (!isFromContact || !IsTOAvailable) {
                dpMap.add(to);
                logger.debug("established directed presence between " + from + " and " + to);
            }
        }

        try {
            stanzaBroker.write(to, redirectDirectedStanza, IgnoreFailureStrategy.INSTANCE);
        } catch (DeliveryException e) {
            logger.warn("relaying directed presence failed. from = " + from + ", to = " + to);
        }

        return null;
    }

    private Set<Entity> getDirectedPresenceMap(SessionContext sessionContext, Entity from) {
        String mapKey = DIRECTED_PRESENCE_MAP + from.getResource();
        Set<Entity> directedPresenceMap = (Set<Entity>) sessionContext.getAttribute(mapKey);
        if (directedPresenceMap == null) {
            directedPresenceMap = new HashSet<Entity>();
            sessionContext.putAttribute(mapKey, directedPresenceMap);
        }
        return directedPresenceMap;
    }

    @SpecCompliant(spec = "RFC3921bis-08", section = "4.5.3")
    private PresenceStanza handleInboundUnavailable(PresenceStanza presenceStanza,
            ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, ResourceRegistry registry) {
        String unavailableContact = "UNKNOWN";
        if (presenceStanza != null && presenceStanza.getFrom() != null) {
            unavailableContact = presenceStanza.getFrom().getFullQualifiedName();
        }
        logger.info("{} has become unavailable", unavailableContact);

        return presenceStanza;
    }

    /**
     * TODO I don't think this works particulary good.
     * 
     * @param stanza
     * @param sessionContext
     * @param rosterManager
     * @param stanzaBroker
     * @return
     */
    @SpecCompliant(spec = "RFC3921bis-08", section = "4.3.2")
    private XMPPCoreStanza handleInboundPresenceProbe(PresenceStanza stanza, SessionContext sessionContext,
            RosterManager rosterManager, StanzaBroker stanzaBroker) {
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
            relayStanza(contact, buildPresenceStanza(user, contact, UNSUBSCRIBED, null), stanzaBroker);
            return null;
        }

        if (contact.getResource() == null) {
            // presence probes must happen on resource level!
            relayStanza(contact, buildPresenceStanza(user, contact, UNSUBSCRIBED, null), stanzaBroker);
            return null;
        }

        PresenceStanza latestPresenceStanza = retrieveLatestPresence(sessionContext, user);
        if (latestPresenceStanza == null) {
            // we have no current presence info
            relayStanza(contact, buildPresenceStanza(user, contact, UNAVAILABLE, null), stanzaBroker);
            return null;
        }

        // return current presence as probing result
        relayStanza(contact, buildPresenceStanza(user, contact, null, latestPresenceStanza.getInnerElements()),
                stanzaBroker);

        return null;
    }

    private void updateLatestPresence(SessionContext sessionContext, Entity user, PresenceStanza stanza) {
        sessionContext.getServerRuntimeContext().getPresenceCache().put(user, stanza);
    }

    private PresenceStanza retrieveLatestPresence(SessionContext sessionContext, Entity user) {
        return sessionContext.getServerRuntimeContext().getPresenceCache().getForBareJID(user.getBareJID());
    }

    @SpecCompliant(spec = "RFC3921bis-08", section = "4.2.3")
    private PresenceStanza handleInboundAvailable(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, ResourceRegistry registry) {

        // TODO ?check if user has blocked contact?

        logger.info("{} has become available", stanza.getFrom().getFullQualifiedName());

        return stanza;
    }

    private void relayTo(Entity from, List<Entity> tos, PresenceStanza original, StanzaBroker stanzaBroker) {
        List<Attribute> toFromReplacements = new ArrayList<Attribute>(2);
        toFromReplacements.add(new Attribute("from", from.getFullQualifiedName()));

        for (Entity to : tos) {
            toFromReplacements.add(new Attribute("to", to.getFullQualifiedName()));
            Stanza outgoingStanza = StanzaBuilder.createClone(original, true, toFromReplacements).build();
            relayStanza(to, outgoingStanza, stanzaBroker);
            toFromReplacements.remove(toFromReplacements.size() - 1); // clear space for new 'to' attribute
        }
    }

}
