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

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.roster.Roster;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;

/**
 * for getting and changing rosters. implementations must transparently handle persistence
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface RosterManager extends StorageProvider {

    public final static String SERVER_SERVICE_ROSTERMANAGER = "rosterManager";

    Roster retrieve(Entity jid) throws RosterException;

    void addContact(Entity jid, RosterItem rosterItem) throws RosterException;

    RosterItem getContact(Entity jidUser, Entity jidContact) throws RosterException;

    void removeContact(Entity jid, Entity jidContact) throws RosterException;

}
