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

import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.NOT_SET;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.BOTH;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.FROM;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.TO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * one contact in the roster of a user, the subscription can either be pending, or established, depending on the values
 * of the subscriptionType and askSubscriptionType fields.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RosterItem {

    /**
     * the contact's JID, eg. "zappa@vysper.org"
     */
    private Entity jid;

    /**
     * a user-chosen, descriptive, often short name ('nick'), eg. "Frank Zappa", or "Frank"
     */
    private String name;

    /**
     * all the groups the item is displayed under. this list can be empty.
     */
    private final List<RosterGroup> groups = new ArrayList<RosterGroup>();

    /**
     * type of subscription either FROM, TO or both. depending on the value of askSubscriptionType, FROM or TO
     * subscriptions might be still pending and awaiting approval 
     */
    private SubscriptionType subscriptionType;

    /**
     * records pending subscriptions, awaiting approval
     */
    private AskSubscriptionType askSubscriptionType;

    public RosterItem(Entity jid, SubscriptionType subscriptionType) {
        this(jid, null, subscriptionType, null);
    }

    public RosterItem(Entity jid, SubscriptionType subscriptionType, AskSubscriptionType askSubscriptionType) {
        this(jid, null, subscriptionType, askSubscriptionType);
    }

    public RosterItem(Entity jid, String name, SubscriptionType subscriptionType,
            AskSubscriptionType askSubscriptionType) {
        this.jid = jid;
        this.name = name;
        this.subscriptionType = subscriptionType;
        this.askSubscriptionType = askSubscriptionType == null ? NOT_SET : askSubscriptionType;
    }

    public RosterItem(Entity jid, String name, SubscriptionType subscriptionType,
            AskSubscriptionType askSubscriptionType, List<RosterGroup> groups) {
        this(jid, name, subscriptionType, askSubscriptionType);
        this.groups.addAll(groups);
    }

    public Entity getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }

    /**
     * sets the user-chosen name for the contact
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * unmodifyable list of groups containing this contact
     * @return list, containing 0..n groups
     */
    public List<RosterGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * the list of groups the contact will be contained in. all previous groups are replaced by the given list.
     * @param newGroups 0..n groups
     */
    public void setGroups(List<RosterGroup> newGroups) {
        this.groups.clear();
        if (newGroups != null)
            this.groups.addAll(newGroups);
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    /**
     * should be set using the RosterSubscriptionMutator
     */
    /*package*/void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public AskSubscriptionType getAskSubscriptionType() {
        return askSubscriptionType;
    }

    /**
     * should be set using the RosterSubscriptionMutator
     */
    /*package*/void setAskSubscriptionType(AskSubscriptionType askSubscribe) {
        this.askSubscriptionType = askSubscribe;
    }

    /**
     * @return TRUE, iff the contact sends presence to the user
     */
    public boolean hasTo() {
        return subscriptionType == TO || subscriptionType == BOTH;
    }

    /**
     * @return TRUE, iff the contact receives presence from the contact
     */
    public boolean hasFrom() {
        return subscriptionType == FROM || subscriptionType == BOTH;
    }

    /**
     * @return TRUE, iff the user and the contact mutually receive each other's presence
     */
    public boolean isBoth() {
        return subscriptionType == BOTH;
    }

    @Override
    public String toString() {
        return "RosterItem{" + "jid=" + (jid == null ? "NULL" : jid.getFullQualifiedName()) + ", name='" + name + '\''
                + ", groups=" + groups + ", subscriptionType=" + subscriptionType + ", askSubscriptionType="
                + askSubscriptionType + '}';
    }
}
