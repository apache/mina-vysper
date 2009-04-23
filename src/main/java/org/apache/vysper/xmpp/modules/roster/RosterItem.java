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

import org.apache.vysper.xmpp.addressing.Entity;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.*;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * one contact in the roster
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class RosterItem {

    private Entity jid;
    private String name;
    private final List<RosterGroup> groups = new ArrayList<RosterGroup>();
    private SubscriptionType subscriptionType;
    private AskSubscriptionType askSubscriptionType;

    public RosterItem(Entity jid, SubscriptionType subscriptionType) {
        this(jid, null, subscriptionType, null);
    }

    public RosterItem(Entity jid, SubscriptionType subscriptionType, AskSubscriptionType askSubscriptionType) {
        this(jid, null, subscriptionType, askSubscriptionType);
    }

    public RosterItem(Entity jid, String name, SubscriptionType subscriptionType, AskSubscriptionType askSubscriptionType) {
        this.jid = jid;
        this.name = name;
        this.subscriptionType = subscriptionType;
        this.askSubscriptionType = askSubscriptionType == null ? NOT_SET : askSubscriptionType;
    }

    public RosterItem(Entity jid, String name, SubscriptionType subscriptionType, AskSubscriptionType askSubscriptionType, List<RosterGroup> groups) {
        this(jid, name, subscriptionType, askSubscriptionType);
        this.groups.addAll(groups);
    }

    public Entity getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RosterGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void setGroups(List<RosterGroup> newGroups) {
        this.groups.clear();
        this.groups.addAll(newGroups);
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    /**
     * should be set using the RosterSubscriptionMutator
     */
     /*package*/ void setSubscriptionType(SubscriptionType subscriptionType) {
         this.subscriptionType = subscriptionType;
    }

    public AskSubscriptionType getAskSubscriptionType() {
        return askSubscriptionType;
    }

    /**
     * should be set using the RosterSubscriptionMutator
     */
    /*package*/ void setAskSubscriptionType(AskSubscriptionType askSubscribe) {
        this.askSubscriptionType = askSubscribe;
    }

    public boolean hasTo() {
        return subscriptionType == TO || subscriptionType == BOTH;
    }

    public boolean hasFrom() {
        return subscriptionType == FROM || subscriptionType == BOTH;
    }

    public boolean isBoth() {
        return subscriptionType == BOTH;
    }

    @Override
    public String toString() {
        return "RosterItem{" +
                "jid=" + (jid == null ? "NULL" : jid.getFullQualifiedName()) +
                ", name='" + name + '\'' +
                ", groups=" + groups +
                ", subscriptionType=" + subscriptionType +
                ", askSubscriptionType=" + askSubscriptionType +
                '}';
    }
}
