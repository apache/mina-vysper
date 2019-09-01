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
package org.apache.vysper.xmpp.modules.roster.handler;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.PARTIAL;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.NONE;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.REMOVE;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.roster.Roster;
import org.apache.vysper.xmpp.modules.roster.RosterBadRequestException;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterNotAcceptableException;
import org.apache.vysper.xmpp.modules.roster.RosterStanzaUtils;
import org.apache.vysper.xmpp.modules.roster.RosterUtils;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
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
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handles roster get, set, push & result requests
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "rfc3921bis-08", section = "2", status = IN_PROGRESS, coverage = COMPLETE)
public class RosterIQHandler extends DefaultIQHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RosterIQHandler.class);

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.JABBER_IQ_ROSTER);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query");
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.2", status = FINISHED, coverage = COMPLETE),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.5", status = FINISHED, coverage = PARTIAL),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.2", status = FINISHED, coverage = COMPLETE) })
    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {

        ResourceRegistry registry = serverRuntimeContext.getResourceRegistry();
        RosterManager rosterManager = serverRuntimeContext.getStorageProvider(RosterManager.class);

        if (rosterManager == null) {
            return handleCannotRetrieveRoster(stanza, sessionContext);
        }

        Entity from = extractUniqueSenderJID(stanza, sessionContext);
        if (from == null || !from.isResourceSet()) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER,
                    stanza, StanzaErrorType.MODIFY,
                    "sender info insufficient: " + ((from == null) ? "no from" : from.getFullQualifiedName()), null,
                    null));
        }
        String resourceId = from.getResource();

        ResourceState currentState = registry.getResourceState(resourceId);
        if (currentState != null) {
            registry.setResourceState(resourceId, ResourceState.makeInterested(currentState));
        }

        Roster roster = null;
        try {
            roster = rosterManager.retrieve(from.getBareJID());
            if (roster == null)
                return handleCannotRetrieveRoster(stanza, sessionContext);
        } catch (RosterException e) {
            return handleCannotRetrieveRoster(stanza, sessionContext);
        }

        // from becomes to
        StanzaBuilder stanzaBuilder = RosterStanzaUtils.createRosterItemsIQ(from, stanza.getID(), IQStanzaType.RESULT,
                roster);
        return Collections.singletonList(stanzaBuilder.build());
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.3", status = FINISHED, coverage = COMPLETE),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.5", status = FINISHED, coverage = PARTIAL),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.6", status = FINISHED, coverage = PARTIAL, comment = "only set-related content applies"),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.5", status = FINISHED, coverage = COMPLETE, comment = "only calling from here") })
    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {
        RosterManager rosterManager = serverRuntimeContext.getStorageProvider(RosterManager.class);

        if (rosterManager == null) {
            return handleCannotRetrieveRoster(stanza, sessionContext);
        }

        Entity user = extractUniqueSenderJID(stanza, sessionContext);
        if (user == null || !user.isResourceSet()) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER,
                    stanza, StanzaErrorType.MODIFY,
                    "sender info insufficient: " + ((user == null) ? "no from" : user.getFullQualifiedName()), null,
                    null));
        }

        RosterItem setRosterItem;
        try {
            setRosterItem = RosterUtils.parseRosterItem(stanza);
        } catch (RosterBadRequestException e) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST,
                    stanza, StanzaErrorType.MODIFY, e.getMessage(), null, null));
        } catch (RosterNotAcceptableException e) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE,
                    stanza, StanzaErrorType.MODIFY, e.getMessage(), null, null));
        }

        Entity contactJid = setRosterItem.getJid().getBareJID();

        RosterItem existingItem;
        try {
            existingItem = rosterManager.getContact(user.getBareJID(), contactJid);
        } catch (RosterException e) {
            existingItem = null;
        }

        if (setRosterItem.getSubscriptionType() == REMOVE) {
            // remove is handled in separate method, return afterwards
            return rosterItemRemove(stanza, sessionContext, rosterManager, user, contactJid, existingItem,
                    stanzaBroker);
        } /*
           * else: all other subscription types are ignored in a roster set and have been
           * filtered out by RosterUtils.parseRosterItem()
           */

        // proper set (update, not a remove)
        if (existingItem == null) {
            existingItem = new RosterItem(contactJid, NONE);
        }

        if (setRosterItem.getName() != null) {
            existingItem.setName(setRosterItem.getName());
            LOG.debug(user.getBareJID() + " roster: set roster item name to " + setRosterItem.getName());
        }
        existingItem.setGroups(setRosterItem.getGroups());
        LOG.debug(user.getBareJID() + " roster: roster item groups set to " + setRosterItem.getGroups());

        try {
            // update contact persistently
            rosterManager.addContact(user.getBareJID(), existingItem);
        } catch (RosterException e) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST,
                    stanza, StanzaErrorType.CANCEL, "roster item contact not (yet) in roster: " + contactJid, null,
                    null));
        }

        pushRosterItemToInterestedResources(sessionContext, user, existingItem, stanzaBroker);

        return Collections.singletonList(
                RosterStanzaUtils.createRosterItemIQ(user, stanza.getID(), IQStanzaType.RESULT, existingItem));
    }

    @SpecCompliant(spec = "rfc3921bis-08", section = "2.5", status = IN_PROGRESS, coverage = COMPLETE, comment = "actual implementation")
    private List<Stanza> rosterItemRemove(IQStanza stanza, SessionContext sessionContext, RosterManager rosterManager,
            Entity user, Entity contactJid, RosterItem existingItem, StanzaBroker stanzaBroker) {
        // rfc3921bis-08/2.5
        Stanza unsubscribedStanza = null;
        Stanza unsubscribeStanza = null;
        if (existingItem != null) {
            if (existingItem.hasFrom()) {
                // send unsubbed
                unsubscribedStanza = StanzaBuilder.createPresenceStanza(user.getBareJID(), contactJid, null,
                        PresenceStanzaType.UNSUBSCRIBED, null, null).build();
            }
            if (existingItem.hasTo()) {
                // send unsub
                unsubscribeStanza = StanzaBuilder.createPresenceStanza(user.getBareJID(), contactJid, null,
                        PresenceStanzaType.UNSUBSCRIBE, null, null).build();
            }
        }
        try {
            rosterManager.removeContact(user.getBareJID(), contactJid);
        } catch (RosterException e) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND,
                    stanza, StanzaErrorType.CANCEL, "roster item contact not in roster: " + contactJid, null, null));
        }

        if (unsubscribedStanza != null) {
            try {
                stanzaBroker.write(contactJid, unsubscribedStanza, IgnoreFailureStrategy.INSTANCE);
            } catch (DeliveryException e) {
                LOG.warn("failure sending unsubscribed on roster remove", e);
            }
        }
        if (unsubscribeStanza != null) {
            try {
                stanzaBroker.write(contactJid, unsubscribeStanza, IgnoreFailureStrategy.INSTANCE);
            } catch (DeliveryException e) {
                LOG.warn("failure sending unsubscribe on roster remove", e);
            }
        }

        // send roster item push to all interested resources
        pushRosterItemToInterestedResources(sessionContext, user, new RosterItem(contactJid, REMOVE), stanzaBroker);

        // return success
        return Collections
                .singletonList(StanzaBuilder.createIQStanza(null, user, IQStanzaType.RESULT, stanza.getID()).build());
    }

    private void pushRosterItemToInterestedResources(SessionContext sessionContext, Entity user, RosterItem rosterItem,
            StanzaBroker stanzaBroker) {
        ResourceRegistry registry = sessionContext.getServerRuntimeContext().getResourceRegistry();
        List<String> resources = registry.getInterestedResources(user.getBareJID());
        for (String resource : resources) {
            Entity userResource = new EntityImpl(user, resource);
            Stanza push = RosterStanzaUtils.createRosterItemPushIQ(userResource, sessionContext.nextSequenceValue(),
                    rosterItem);

            try {
                stanzaBroker.write(userResource, push, IgnoreFailureStrategy.INSTANCE);
            } catch (DeliveryException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    protected List<Stanza> handleCannotRetrieveRoster(IQStanza stanza, SessionContext sessionContext) {
        throw new RuntimeException("gracefully handling roster management problem not implemented");
    }

}
