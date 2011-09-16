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
package org.apache.vysper.xmpp.modules.roster.persistence;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.roster.MutableRoster;
import org.apache.vysper.xmpp.modules.roster.Roster;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractRosterManager implements RosterManager, ServerRuntimeContextService {

    abstract protected Roster retrieveRosterInternal(Entity bareJid);

    abstract protected Roster addNewRosterInternal(Entity jid);

    public Roster retrieve(Entity jid) {
        jid = jid.getBareJID();
        return retrieveRosterInternal(jid);
    }

    public void addContact(Entity jid, RosterItem rosterItem) throws RosterException {
        if (jid == null)
            throw new RosterException("jid not provided");
        final Roster roster = retrieve(jid);
        if (!(roster instanceof MutableRoster)) throw new RosterException("roster is not mutable");
        MutableRoster mutableRoster = (MutableRoster)roster;
        if (mutableRoster == null) {
            mutableRoster = (MutableRoster) addNewRosterInternal(jid);
        }
        mutableRoster.addItem(rosterItem);
    }

    public RosterItem getContact(Entity jidUser, Entity jidContact) throws RosterException {
        if (jidUser == null)
            throw new RosterException("jid not provided");
        Roster roster = retrieve(jidUser);
        if (roster == null)
            throw new RosterException("roster not available for jid = " + jidUser.getFullQualifiedName());
        return roster.getEntry(jidContact);
    }

    public void removeContact(Entity jidUser, Entity jidContact) throws RosterException {
        if (jidUser == null)
            throw new RosterException("jid not provided");
        Roster roster = retrieve(jidUser);
        if (roster == null)
            throw new RosterException("roster not available for jid = " + jidUser.getFullQualifiedName());
        if (!(roster instanceof MutableRoster)) throw new RosterException("roster is not mutable");
        MutableRoster mutableRoster = (MutableRoster)roster;
        final boolean success = mutableRoster.removeItem(jidContact);
    }

    public String getServiceName() {
        return RosterManager.SERVER_SERVICE_ROSTERMANAGER;
    }
}
