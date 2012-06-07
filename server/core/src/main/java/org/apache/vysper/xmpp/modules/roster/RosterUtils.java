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
package org.apache.vysper.xmpp.modules.roster;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.PARTIAL;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.stanza.IQStanza;

/**
 * some roster logic
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RosterUtils {

    /**
     * takes the roster of a user and groups items by subscription state. this is helpful when all FROM items
     * are needed and then all TO items - but the roster is only iterated once. 
     */
    public static Map<SubscriptionType, List<RosterItem>> getRosterItemsByState(RosterManager rosterManager, Entity user) {
        
        Map<SubscriptionType, List<RosterItem>> rosterItemMap = new HashMap<SubscriptionType, List<RosterItem>>();

        rosterItemMap.put(SubscriptionType.FROM, new ArrayList<RosterItem>());
        rosterItemMap.put(SubscriptionType.TO, new ArrayList<RosterItem>());
        rosterItemMap.put(SubscriptionType.BOTH, new ArrayList<RosterItem>());
        rosterItemMap.put(SubscriptionType.REMOVE, new ArrayList<RosterItem>());
        rosterItemMap.put(SubscriptionType.NONE, new ArrayList<RosterItem>());

        if (rosterManager == null) return rosterItemMap;
        
        Roster roster;
        try {
            roster = rosterManager.retrieve(user);
        } catch (RosterException e) {
            // TODO: make this errorhandling more intelligent
            throw new RuntimeException("could not retrieve roster for user " + user.getFullQualifiedName());
        }

        // get items sorted by subscription type
        for (RosterItem rosterItem : roster) {
            rosterItemMap.get(rosterItem.getSubscriptionType()).add(rosterItem);
        }

        return rosterItemMap;
    }

    /**
     * extracts a roster item from the given stanza
     */
    public static RosterItem parseRosterItem(IQStanza stanza) throws RosterBadRequestException,
            RosterNotAcceptableException {
        return parseRosterItem(stanza, false); // do not read subscription types (except 'remove')
    }

    /**
     * extracts a roster item from the given stanza, with relaxed semantical checks for testing
     */
    public static RosterItem parseRosterItemForTesting(IQStanza stanza) throws RosterBadRequestException,
            RosterNotAcceptableException {
        return parseRosterItem(stanza, true); // do also parse subscription types
    }

    /**
     * extracts a roster item from the stanza and checks for integrity according to the XMPP spec
     */
    @SpecCompliance(compliant = {
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.1", status = FINISHED, coverage = COMPLETE),
            @SpecCompliant(spec = "rfc3921bis-08", section = "2.1.3", status = FINISHED, coverage = PARTIAL, comment = "handles the conformance rules 1-3 when parseSubscriptionTypes is set to false") })
    private static RosterItem parseRosterItem(IQStanza stanza, boolean parseSubscriptionTypes)
            throws RosterBadRequestException, RosterNotAcceptableException {
        XMLElement queryElement;
        try {
            queryElement = stanza.getSingleInnerElementsNamed("query");
            if (queryElement == null)
                throw new XMLSemanticError("missing query node");
        } catch (XMLSemanticError xmlSemanticError) {
            throw new RosterBadRequestException("roster set needs a single query node.");
        }

        XMLElement itemElement;
        try {
            itemElement = queryElement.getSingleInnerElementsNamed("item");
            if (itemElement == null)
                throw new XMLSemanticError("missing item node");
        } catch (XMLSemanticError xmlSemanticError) {
            throw new RosterBadRequestException("roster set needs a single item node.");
        }

        Attribute attributeJID = itemElement.getAttribute("jid");
        if (attributeJID == null || attributeJID.getValue() == null)
            throw new RosterBadRequestException("missing 'jid' attribute on item node");

        XMLElementVerifier verifier = itemElement.getVerifier();
        String name = verifier.attributePresent("name") ? itemElement.getAttribute("name").getValue() : null;
        if (name != null && name.length() > RosterConfiguration.ROSTER_ITEM_NAME_MAX_LENGTH) {
            throw new RosterNotAcceptableException("roster name too long: " + name.length());
        }

        SubscriptionType subscription = verifier.attributePresent("subscription") ? SubscriptionType
                .valueOf(itemElement.getAttribute("subscription").getValue().toUpperCase()) : SubscriptionType.NONE;
        if (!parseSubscriptionTypes && subscription != SubscriptionType.REMOVE)
            subscription = SubscriptionType.NONE; // roster remove is always tolerated

        AskSubscriptionType askSubscriptionType = AskSubscriptionType.NOT_SET;
        if (parseSubscriptionTypes) {
            askSubscriptionType = verifier.attributePresent("ask") ? AskSubscriptionType.valueOf("ASK_"
                    + itemElement.getAttribute("ask").getValue().toUpperCase()) : AskSubscriptionType.NOT_SET;
        }

        String contactJid = attributeJID.getValue();
        Entity contact;
        try {
            contact = EntityImpl.parse(contactJid);
        } catch (EntityFormatException e) {
            throw new RosterNotAcceptableException("jid cannot be parsed: " + contactJid);
        }

        List<RosterGroup> groups = new ArrayList<RosterGroup>();
        List<XMLElement> groupElements = itemElement.getInnerElementsNamed("group");
        if (groupElements != null) {
            for (XMLElement groupElement : groupElements) {
                String groupName = null;
                try {
                    groupName = groupElement.getSingleInnerText().getText();
                } catch (XMLSemanticError xmlSemanticError) {
                    throw new RosterBadRequestException("roster item group node is malformed");
                }
                if (StringUtils.isEmpty(groupName)) {
                    throw new RosterNotAcceptableException("roster item group name of zero length");
                } else if (groupName.length() > RosterConfiguration.ROSTER_GROUP_NAME_MAX_LENGTH) {
                    throw new RosterNotAcceptableException("roster item group name too long: " + groupName.length());
                }
                RosterGroup group = new RosterGroup(groupName);
                if (groups.contains(group) && !RosterConfiguration.ROSTER_ITEM_GROUP_ALLOW_DUPLICATES) {
                    throw new RosterNotAcceptableException("duplicate roster group name: " + groupName);
                } else {
                    groups.add(group);
                }
            }
        }

        RosterItem rosterItem = new RosterItem(contact, name, subscription, askSubscriptionType, groups);
        return rosterItem;
    }
}
