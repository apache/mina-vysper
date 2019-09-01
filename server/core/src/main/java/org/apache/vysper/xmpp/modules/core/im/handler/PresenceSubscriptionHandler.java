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
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.NOT_STARTED;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.ASK_SUBSCRIBE;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.ASK_SUBSCRIBED;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.ALREADY_SET;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.OK;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.FROM;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.NONE;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.TO;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.SUBSCRIBED;
import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.isSubscriptionType;

import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterStanzaUtils;
import org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanzaVerifier;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handling presence stanzas of type subscription
 *
 * TODO: review all the printStackTraces and throws and turn them into logs or
 * stanza errors
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PresenceSubscriptionHandler extends AbstractPresenceSpecializedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PresenceSubscriptionHandler.class);

    @Override
    /* package */Stanza executeCorePresence(ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, PresenceStanza presenceStanza, RosterManager rosterManager,
            StanzaBroker stanzaBroker) {

        if (!isSubscriptionType(presenceStanza.getPresenceType())) {
            throw new RuntimeException(
                    "case not handled in availability handler" + presenceStanza.getPresenceType().value());
        }

        // TODO: either use the resource associated with the session
        // (initiatingEntity)
        // or in case of multiple resources, use the from attribute or return an
        // error if the from attribute is not present.
        Entity initiatingEntity = null;
        if (sessionContext != null) {
            if (sessionContext.isServerToServer()) {
                initiatingEntity = presenceStanza.getFrom();
            } else {
                initiatingEntity = sessionContext.getInitiatingEntity();
            }
        }

        XMPPCoreStanzaVerifier verifier = presenceStanza.getCoreVerifier();
        ResourceRegistry registry = serverRuntimeContext.getResourceRegistry();

        PresenceStanzaType type = presenceStanza.getPresenceType();

        if (isOutboundStanza) {
            // this is an outbound subscription
            // request/approval/cancellation/unsubscription
            // stamp it with the bare JID of the user
            Entity user = initiatingEntity;
            PresenceStanza stampedStanza = buildPresenceStanza(user.getBareJID(), presenceStanza.getTo().getBareJID(),
                    presenceStanza.getPresenceType(), null);

            switch (type) {

            case SUBSCRIBE:
                // RFC3921bis-04#3.1.2
                // user requests subsription to contact
                handleOutboundSubscriptionRequest(stampedStanza, sessionContext, registry, rosterManager, stanzaBroker);
                break;

            case SUBSCRIBED:
                // RFC3921bis-04#3.1.5
                // user approves subscription to requesting contact
                handleOutboundSubscriptionApproval(stampedStanza, sessionContext, registry, rosterManager,
                        stanzaBroker);
                break;

            case UNSUBSCRIBE:
                // RFC3921bis-04#3.3.2
                // user removes subscription from contact
                handleOutboundUnsubscription(stampedStanza, sessionContext, registry, rosterManager, stanzaBroker);
                break;

            case UNSUBSCRIBED:
                // RFC3921bis-04#3.2.2
                // user approves unsubscription of contact
                handleOutboundSubscriptionCancellation(stampedStanza, sessionContext, registry, rosterManager,
                        stanzaBroker);
                break;

            default:
                throw new RuntimeException("unhandled case " + type.value());
            }

        } else /* inbound */ {

            switch (type) {

            case SUBSCRIBE:
                // RFC3921bis-04#3.1.3
                // contact requests subscription to user
                return handleInboundSubscriptionRequest(presenceStanza, sessionContext, rosterManager, stanzaBroker);

            case SUBSCRIBED:
                // RFC3921bis-04#3.1.6
                // contact approves user's subsription request
                return handleInboundSubscriptionApproval(presenceStanza, sessionContext, registry, rosterManager,
                        stanzaBroker);

            case UNSUBSCRIBE:
                // RFC3921bis-04#3.3.3
                // contact unsubscribes
                handleInboundUnsubscription(presenceStanza, serverRuntimeContext, sessionContext, registry,
                        rosterManager, stanzaBroker);
                return null;

            case UNSUBSCRIBED:
                // RFC3921bis-04#3.2.3
                // contact denies subsription
                handleInboundSubscriptionCancellation(presenceStanza, sessionContext, registry, rosterManager,
                        stanzaBroker);
                return null;

            default:
                throw new RuntimeException("unhandled case " + type.value());

            }
        }
        return null;
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.3.3", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.3.3", status = NOT_STARTED, comment = "substantial additions from bis-05 not yet taken into account") })
    protected void handleInboundUnsubscription(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, ResourceRegistry registry, RosterManager rosterManager,
            StanzaBroker stanzaBroker) {
        Entity contact = stanza.getFrom();
        Entity user = stanza.getTo();

        Entity userBareJid = user.getBareJID();
        Entity contactBareJid = contact.getBareJID();

        RosterItem rosterItem;
        try {
            rosterItem = rosterManager.getContact(userBareJid, contactBareJid);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (rosterItem == null)
            return;

        RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().remove(rosterItem, FROM);

        if (result != OK) {
            // TODO
            return;
        }

        // send roster push to all interested resources
        // TODO do this only once, since inbound is multiplexed on
        // DeliveringInboundStanzaRelay level already
        List<String> resources = registry.getInterestedResources(user);
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

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.3.2", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.3.2", status = NOT_STARTED, comment = "rephrasing from bis-05 not yet taken into account") })
    protected void handleOutboundUnsubscription(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {
        Entity user = stanza.getFrom();
        Entity contact = stanza.getTo();

        Entity userBareJid = user.getBareJID();
        Entity contactBareJid = contact.getBareJID();

        relayStanza(contact, stanza, stanzaBroker);

        RosterItem rosterItem;
        try {
            rosterItem = rosterManager.getContact(userBareJid, contactBareJid);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (rosterItem == null)
            return;

        RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().remove(rosterItem, TO);

        if (result != OK) {
            // TODO
            return;
        }

        relayStanza(contact, stanza, stanzaBroker);

        sendRosterUpdate(sessionContext, registry, user, rosterItem, stanzaBroker);
    }

    /**
     * send roster push to all of the user's interested resources
     */
    protected void sendRosterUpdate(SessionContext sessionContext, ResourceRegistry registry, Entity user,
            RosterItem rosterItem, StanzaBroker stanzaBroker) {
        List<String> resources = registry.getInterestedResources(user);

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

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.2.3", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.2.3", status = NOT_STARTED, comment = "additions from bis-05 not yet taken into account") })
    protected void handleInboundSubscriptionCancellation(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {

        Entity contact = stanza.getFrom();
        Entity user = stanza.getTo();

        Entity userBareJid = user.getBareJID();
        Entity contactBareJid = contact.getBareJID();

        RosterItem rosterItem;
        try {
            rosterItem = rosterManager.getContact(userBareJid, contactBareJid);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (rosterItem == null)
            return;

        RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().remove(rosterItem, TO);

        if (result != OK) {
            // TODO
            return;
        }

        // send roster push to all interested resources
        // TODO do this only once, since inbound is multiplexed on
        // DeliveringInboundStanzaRelay level already
        List<String> resources = registry.getInterestedResources(user);
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

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.2.2", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.2.2", status = NOT_STARTED, comment = "rephrasing from bis-05 not yet taken into account") })
    protected void handleOutboundSubscriptionCancellation(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {
        Entity user = stanza.getFrom();
        Entity contact = stanza.getTo();

        Entity userBareJid = user.getBareJID();
        Entity contactBareJid = contact.getBareJID();

        RosterItem rosterItem = null;
        try {
            rosterItem = rosterManager.getContact(userBareJid, contactBareJid);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (rosterItem == null)
            return;

        RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().remove(rosterItem, FROM);

        if (result != OK) {
            // TODO
            return;
        }

        relayStanza(contact, stanza, stanzaBroker);

        // send roster push to all of the user's interested resources
        sendRosterUpdate(sessionContext, registry, user, rosterItem, stanzaBroker);
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.1.5", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.1.5", status = NOT_STARTED, comment = "slight changes from bis-05 not yet taken into account") })
    protected void handleOutboundSubscriptionApproval(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {
        Entity user = stanza.getFrom();
        Entity contact = stanza.getTo();

        Entity userBareJid = user.getBareJID();
        Entity contactBareJid = contact.getBareJID();

        RosterItem rosterItem = null;
        try {
            rosterItem = getExistingOrNewRosterItem(rosterManager, userBareJid, contactBareJid);

            RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().add(rosterItem, FROM);
            if (result != OK) {
                // TODO
                return;
            }

            rosterManager.addContact(userBareJid, rosterItem);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        relayStanza(contact, stanza, stanzaBroker);

        // send roster push to all of the user's interested resources
        sendRosterUpdate(sessionContext, registry, user, rosterItem, stanzaBroker);

        // send presence information from user's available resource to the
        // contact
        List<String> resources = registry.getAvailableResources(user);
        for (String resource : resources) {
            Entity userResource = new EntityImpl(user, resource);

            PresenceStanza cachedPresenceStanza = sessionContext.getServerRuntimeContext().getPresenceCache()
                    .get(userResource);
            if (cachedPresenceStanza == null)
                continue;

            PresenceStanza sendoutPresence = buildPresenceStanza(userResource, contactBareJid, null,
                    cachedPresenceStanza.getInnerElements());
            relayStanza(contact, sendoutPresence, stanzaBroker);
        }
    }

    private RosterItem getExistingOrNewRosterItem(RosterManager rosterManager, Entity userJid, Entity contactJid)
            throws RosterException {
        RosterItem rosterItem = rosterManager.getContact(userJid, contactJid);
        if (rosterItem == null) {
            rosterItem = new RosterItem(contactJid, NONE);
        }
        return rosterItem;
    }

    /**
     * TODO this handling method should be optimized to be processed only once for
     * every session DeliveringInboundStanzaRelay call this for every resource
     * separately
     */
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.1.6", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.1.6", status = NOT_STARTED, comment = "minor rephrasing from bis-05 not yet taken into account") })
    protected Stanza handleInboundSubscriptionApproval(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {

        Entity contact = stanza.getFrom();
        Entity user = stanza.getTo();

        Entity userBareJid = user.getBareJID();

        RosterItem rosterItem;
        RosterSubscriptionMutator.Result result;
        try {
            rosterItem = getExistingOrNewRosterItem(rosterManager, userBareJid, contact);

            result = RosterSubscriptionMutator.getInstance().add(rosterItem, TO);

            rosterManager.addContact(userBareJid, rosterItem);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (result == OK || result == ALREADY_SET) {

            // send roster push to all interested resources
            // TODO do this only once, since inbound is multiplexed on
            // DeliveringInboundStanzaRelay level already
            List<String> resources = registry.getInterestedResources(user);
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
        } else {
            // silently drop the stanza
        }

        return null;
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.1.3", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.1.3", status = NOT_STARTED, comment = "major rephrasing from bis-05 not yet taken into account") })
    protected Stanza handleInboundSubscriptionRequest(PresenceStanza stanza, SessionContext sessionContext,
            RosterManager rosterManager, StanzaBroker stanzaBroker) {
        Entity contact = stanza.getFrom();
        Entity user = stanza.getTo();

        Entity userBareJid = user.getBareJID();

        RosterItem rosterItem;
        RosterSubscriptionMutator.Result result;
        try {
            rosterItem = getExistingOrNewRosterItem(rosterManager, userBareJid, contact);

            result = RosterSubscriptionMutator.getInstance().add(rosterItem, ASK_SUBSCRIBED);

            rosterManager.addContact(userBareJid, rosterItem);
        } catch (RosterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // check whether user already has a subscription to contact
        if (result == ALREADY_SET) {
            Entity receiver = contact.getBareJID();
            PresenceStanza alreadySubscribedResponse = buildPresenceStanza(userBareJid, receiver, SUBSCRIBED, null);
            relayStanza(receiver, alreadySubscribedResponse, stanzaBroker);
            return null;
        }

        // user exists and doesn't have a subscription, so...

        // TODO check if user has blocked contact

        // write inbound subscription request to the user
        stanzaBroker.writeToSession(stanza);

        return null;
    }

    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "RFC3921bis-05", section = "3.1.2", status = IN_PROGRESS, comment = "current impl based hereupon"),
            @SpecCompliant(spec = "RFC3921bis-08", section = "3.1.2", status = NOT_STARTED, comment = "major rephrasing from bis-05 not yet taken into account") })
    private void handleOutboundSubscriptionRequest(PresenceStanza stanza, SessionContext sessionContext,
            ResourceRegistry registry, RosterManager rosterManager, StanzaBroker stanzaBroker) {

        Entity user = stanza.getFrom();
        Entity contact = stanza.getTo().getBareJID();

        // TODO schedule a observer which can re-send the request

        RosterItem rosterItem;
        try {
            rosterItem = getExistingOrNewRosterItem(rosterManager, user.getBareJID(), contact);

            RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().add(rosterItem,
                    ASK_SUBSCRIBE);
            if (result != OK) {
                return;
            }

            rosterManager.addContact(user.getBareJID(), rosterItem);
        } catch (RosterException e) {
            throw new RuntimeException(e);
        }

        // relay the stanza to the contact (via the contact's server)
        try {
            stanzaBroker.write(stanza.getTo(), stanza, IgnoreFailureStrategy.INSTANCE);
        } catch (DeliveryException e) {
            e.printStackTrace();
        }

        // send roster push to all of the user's interested resources
        List<String> resources = registry.getInterestedResources(user);
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

}
