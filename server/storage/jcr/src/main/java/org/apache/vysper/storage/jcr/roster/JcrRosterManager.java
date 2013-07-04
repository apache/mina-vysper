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
package org.apache.vysper.storage.jcr.roster;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.vysper.storage.jcr.JcrStorage;
import org.apache.vysper.storage.jcr.JcrStorageException;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.roster.AskSubscriptionType;
import org.apache.vysper.xmpp.modules.roster.MutableRoster;
import org.apache.vysper.xmpp.modules.roster.Roster;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterGroup;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.AbstractRosterManager;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * roster items are stored for contacts in the following path:
 * /accountentity/user@vysper.org/jabber_iq_roster/contact@vysper.org
 * all item properties besides contact jid (which is used as a node name)
 * are stored as node properties
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JcrRosterManager extends AbstractRosterManager {

    final Logger logger = LoggerFactory.getLogger(JcrRosterManager.class);

    protected JcrStorage jcrStorage;

    public JcrRosterManager(JcrStorage jcrStorage) {
        this.jcrStorage = jcrStorage;
    }

    /*package*/static Node retrieveRosterNode(JcrStorage jcrStorage, Entity bareJid) {
        try {
            if (jcrStorage.getEntityNode(bareJid, null, false) == null)
                return null;
            return jcrStorage.getEntityNode(bareJid, NamespaceURIs.JABBER_IQ_ROSTER, true);
        } catch (JcrStorageException e) {
            return null;
        }
    }

    @Override
    protected Roster retrieveRosterInternal(Entity bareJid) {
        final Node rosterNode = retrieveRosterNode(jcrStorage, bareJid);

        MutableRoster roster = new MutableRoster();

        NodeIterator nodes = null;
        try {
            nodes = rosterNode.getNodes();
        } catch (RepositoryException e) {
            return roster; // empty roster object
        }
        while (nodes != null && nodes.hasNext()) {
            Node node = nodes.nextNode();

            String contactJidString = null;
            try {
                contactJidString = node.getName();
            } catch (RepositoryException e) {
                logger.warn("when loading roster for user {} cannot read node name for node id = " + node.toString());
            }
            logger.warn("try now loading contact " + contactJidString + " from node " + node.toString());
            EntityImpl contactJid = null;
            if (contactJidString != null) {
                try {
                    contactJid = EntityImpl.parse(contactJidString);
                } catch (EntityFormatException e) {
                    logger.warn("when loading roster for user {} parsing  contact jid {}", bareJid, contactJidString);
                }
            }
            if (contactJid == null) {
                logger.warn("when loading roster for user {}, skipping a contact due to missing or unparsable jid",
                        bareJid);
                continue;
            }

            String name = readAttribute(node, "name");
            String typeString = readAttribute(node, "type");
            SubscriptionType subscriptionType = null;
            try {
                subscriptionType = SubscriptionType.valueOf(typeString == null ? "NONE" : typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("when loading roster for user " + bareJid + ", contact " + contactJid
                        + " misses a subscription type", bareJid, contactJid);
            }
            String askTypeString = readAttribute(node, "askType");
            AskSubscriptionType askSubscriptionType = AskSubscriptionType.NOT_SET;
            try {
                if (askTypeString != null)
                    askSubscriptionType = AskSubscriptionType.valueOf(askTypeString);
            } catch (IllegalArgumentException e) {
                logger.warn("when loading roster for user " + bareJid.getFullQualifiedName() + ", contact "
                        + contactJid.getFullQualifiedName() + ", the ask subscription type is unparsable. skipping!");
                continue; // don't return it, don't set a default!
            }

            List<RosterGroup> groups = new ArrayList<RosterGroup>();
            // TODO read groups

            RosterItem item = new RosterItem(contactJid, name, subscriptionType, askSubscriptionType, groups);
            logger.info("item loaded for " + bareJid.getFullQualifiedName() + ": " + item.toString());
            roster.addItem(item);
        }
        return roster;
    }

    private String readAttribute(Node node, String propertyName) {
        try {
            Property property = node.getProperty(propertyName);
            if (property == null)
                return null;
            return property.getString();
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Override
    protected Roster addNewRosterInternal(Entity jid) {
        return new MutableRoster();
    }

    @Override
    public void addContact(Entity jid, RosterItem rosterItem) throws RosterException {
        if (jid == null)
            throw new RosterException("jid not provided");
        if (rosterItem.getJid() == null)
            throw new RosterException("contact jid not provided");

        // TODO think about concurrent updates

        Entity contactJid = rosterItem.getJid().getBareJID();
        Node contactNode = getOrCreateContactNode(jid, contactJid);
        try {
            setOrRemoveAttribute(contactNode, "name", rosterItem.getName());
            String subscriptionTypeValue = rosterItem.getSubscriptionType() == null ? null : rosterItem
                    .getSubscriptionType().value();
            setOrRemoveAttribute(contactNode, "type", subscriptionTypeValue);
            String askSubscriptionTypeValue = null;
            if (rosterItem.getAskSubscriptionType() != null
                    && rosterItem.getAskSubscriptionType() != AskSubscriptionType.NOT_SET) {
                askSubscriptionTypeValue = rosterItem.getAskSubscriptionType().value();
            }
            setOrRemoveAttribute(contactNode, "askType", askSubscriptionTypeValue);
            contactNode.save();
            logger.info("JCR node created/updated: " + contactNode);
        } catch (RepositoryException e) {
            throw new RosterException("failed to add contact node to roster for user = " + jid.getFullQualifiedName()
                    + " and contact jid = " + rosterItem.getJid().getFullQualifiedName(), e);
        }
    }

    private void setOrRemoveAttribute(Node contactNode, String attributeName, String attributeValue)
            throws RepositoryException {
        if (attributeValue != null)
            contactNode.setProperty(attributeName, attributeValue);
        else if (contactNode.hasProperty(attributeName))
            contactNode.setProperty(attributeName, (String) null);
    }

    private Node getOrCreateContactNode(Entity jid, Entity contactJid) throws RosterException {
        Node entityNode = null;
        try {
            entityNode = jcrStorage.getEntityNode(jid, NamespaceURIs.JABBER_IQ_ROSTER, true);
        } catch (JcrStorageException e) {
            throw new RosterException("failed to create roster store for " + jid.getFullQualifiedName(), e);
        }
        Node contactNode = null;
        try {
            contactNode = entityNode.getNode(contactJid.getFullQualifiedName());
        } catch (RepositoryException e) {
            // not exists, create
            try {
                contactNode = entityNode.addNode(contactJid.getFullQualifiedName());
                entityNode.save();
            } catch (RepositoryException addNodeEx) {
                throw new RosterException("failed to add contact node to roster for user = "
                        + jid.getFullQualifiedName() + " and contact jid = " + contactJid.getFullQualifiedName(),
                        addNodeEx);
            }

        }
        return contactNode;
    }

    @Override
    public void removeContact(Entity jidUser, Entity jidContact) throws RosterException {
        if (jidUser == null)
            throw new RosterException("jid not provided");
        if (jidContact == null)
            throw new RosterException("contact jid not provided");
        Node rosterNode = null;
        try {
            rosterNode = jcrStorage.getEntityNode(jidUser, NamespaceURIs.JABBER_IQ_ROSTER, false);
        } catch (JcrStorageException e) {
            throw new RosterException("failed to retrieve roster store for " + jidUser.getFullQualifiedName(), e);
        }
        if (rosterNode == null)
            return; // done, no contacts anyway. oops

        NodeIterator nodes = null;
        try {
            nodes = rosterNode.getNodes("contact");
        } catch (RepositoryException e) {
            return; // failed to find any contacts, done.
        }
        boolean foundOne = false;
        while (nodes != null && nodes.hasNext()) {
            Node node = nodes.nextNode();
            String contactJidString = readAttribute(node, "jid");
            if (contactJidString != null && contactJidString.equals(jidContact.getFullQualifiedName())) {
                foundOne = true;
                try {
                    node.remove();
                } catch (RepositoryException e) {
                    logger.warn("failed to remove from roster for user {} the contact jid " + jidContact, jidUser, e);
                }
            }
        }
        if (!foundOne)
            logger.warn("failed to remove from roster for user " + jidUser + " the contact jid " + jidContact);
    }
}
