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
package org.apache.vysper.storage.hbase.roster;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.vysper.storage.hbase.HBaseStorage;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.roster.AskSubscriptionType;
import org.apache.vysper.xmpp.modules.roster.MutableRoster;
import org.apache.vysper.xmpp.modules.roster.Roster;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterGroup;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.AbstractRosterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import static org.apache.vysper.storage.hbase.HBaseStorage.*;
import static org.apache.vysper.storage.hbase.HBaseUtils.asBytes;
import static org.apache.vysper.storage.hbase.HBaseUtils.entityAsBytes;
import static org.apache.vysper.storage.hbase.HBaseUtils.toStr;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HBaseRosterManager extends AbstractRosterManager {

    final Logger LOG = LoggerFactory.getLogger(HBaseRosterManager.class);
    
    public static final String COLUMN_PREFIX_NAME = "n:";
    public static final String COLUMN_PREFIX_TYPE = "t:";
    public static final String COLUMN_PREFIX_ASKTYPE = "a:";
    public static final String COLUMN_PREFIX_GROUP = "g:";

    protected HBaseStorage hBaseStorage;

    public HBaseRosterManager(HBaseStorage hBaseStorage) {
        this.hBaseStorage = hBaseStorage;
    }

    @Override
    protected Roster retrieveRosterInternal(Entity bareJid) {
        final Result entityRow = hBaseStorage.getEntityRow(bareJid, COLUMN_FAMILY_NAME_CONTACT, COLUMN_FAMILY_NAME_ROSTER);

        MutableRoster roster = new MutableRoster();

        final NavigableMap<byte[],byte[]> contacts = entityRow.getFamilyMap(COLUMN_FAMILY_NAME_CONTACT_BYTES);
        if (contacts == null) return roster;
        
        for (byte[] contactBytes : contacts.keySet()) {
            String contactAsString = null;
            EntityImpl contactJID = null;
            try {
                contactAsString = new String(contactBytes, "UTF-8");
                contactJID = EntityImpl.parse(contactAsString);
            } catch (Exception e) {
                LOG.warn("failed to read contact identified by '{}' for user {}", bareJid, contactAsString);
                continue;
            }

            final NavigableMap<byte[],byte[]> contactDetails = entityRow.getFamilyMap(COLUMN_FAMILY_NAME_ROSTER_BYTES);
            String name = toStr(contactDetails.get(asBytes(COLUMN_PREFIX_NAME + contactAsString)));
            String typeString = toStr(contactDetails.get(asBytes(COLUMN_PREFIX_TYPE + contactAsString)));
            String askTypeString = toStr(contactDetails.get(asBytes(COLUMN_PREFIX_ASKTYPE + contactAsString)));

            SubscriptionType subscriptionType = null;
            try {
                subscriptionType = SubscriptionType.valueOf(typeString == null ? "NONE" : typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOG.warn("when loading roster for user " + bareJid + ", contact " + contactJID + " misses a subscription type");
            }

            AskSubscriptionType askSubscriptionType = AskSubscriptionType.NOT_SET;
            try {
                if (StringUtils.isNotBlank(askTypeString)) {
                    askSubscriptionType = AskSubscriptionType.valueOf(askTypeString);
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("when loading roster for user " + bareJid.getFullQualifiedName() + ", contact "
                        + contactJID.getFullQualifiedName() + ", the ask subscription type '" + askTypeString + "' is unparsable. skipping!");
                continue; // don't return it, don't set a default!
            }

            List<RosterGroup> groups = new ArrayList<RosterGroup>();
            int i = 1;
            while (true) {
                String columnName = COLUMN_PREFIX_GROUP + i + ":" + contactAsString;
                String groupName = toStr(contactDetails.get(asBytes(columnName)));
                if (groupName == null) break;

                groups.add(new RosterGroup(groupName));
                i++;
            }

            RosterItem item = new RosterItem(contactJID, name, subscriptionType, askSubscriptionType, groups);
            LOG.info("item loaded for " + bareJid.getFullQualifiedName() + ": " + item.toString());
            roster.addItem(item);
        }
        return roster;
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

        Entity contactJid = rosterItem.getJid().getBareJID();
        final String contactIdentifier = contactJid.getFullQualifiedName();
        
        // prepare contact entries
        final Put put = new Put(entityAsBytes(jid.getBareJID()));
        put.add(COLUMN_FAMILY_NAME_CONTACT_BYTES, asBytes(contactIdentifier), asBytes(rosterItem.getSubscriptionType().value()));
        put.add(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_NAME + contactIdentifier), asBytes(rosterItem.getName()));
        put.add(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_TYPE + contactIdentifier), asBytes(rosterItem.getSubscriptionType().name()));
        put.add(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_ASKTYPE + contactIdentifier), asBytes(rosterItem.getAskSubscriptionType().name()));
        int i = 1;
        for (RosterGroup rosterGroup : rosterItem.getGroups()) {
            String columnName = COLUMN_PREFIX_GROUP + i + ":" + contactIdentifier; 
            put.add(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(columnName), asBytes(rosterGroup.getName()));
            i++;
        }

        HTableInterface userTable = null;
        try {
            userTable = hBaseStorage.getTable(TABLE_NAME_USER);
            userTable.put(put);
            LOG.info("contact {} saved to HBase for user {}", rosterItem.getJid(), jid);
        } catch (IOException e) {
            throw new RosterException("failed to add contact node to roster for user = " + jid.getFullQualifiedName()
                    + " and contact jid = " + rosterItem.getJid().getFullQualifiedName(), e);
        } finally {
            hBaseStorage.putTable(userTable);
        }
    }

    @Override
    public void removeContact(Entity jidUser, Entity jidContact) throws RosterException {
        if (jidUser == null)
            throw new RosterException("jid not provided");
        if (jidContact == null)
            throw new RosterException("contact jid not provided");

        final String contactIdentifier = jidContact.getFullQualifiedName();
        final Delete delete = new Delete(entityAsBytes(jidUser.getBareJID()));
        delete.deleteColumns(COLUMN_FAMILY_NAME_CONTACT_BYTES, asBytes(contactIdentifier));
        delete.deleteColumns(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_NAME + contactIdentifier));
        delete.deleteColumns(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_TYPE + contactIdentifier));
        delete.deleteColumns(COLUMN_FAMILY_NAME_ROSTER_BYTES, asBytes(COLUMN_PREFIX_ASKTYPE + contactIdentifier));
        
        HTableInterface userTable = null;
        try {
            userTable = hBaseStorage.getTable(TABLE_NAME_USER);
            userTable.delete(delete);
            LOG.info("contact {} removed from HBase for user {}", jidContact, jidUser);
        } catch (IOException e) {
            throw new RosterException("failed to add contact node to roster for user = " + jidUser.getFullQualifiedName()
                    + " and contact jid = " + jidContact.getFullQualifiedName(), e);
        } finally {
            hBaseStorage.putTable(userTable);
        }
    }
}
