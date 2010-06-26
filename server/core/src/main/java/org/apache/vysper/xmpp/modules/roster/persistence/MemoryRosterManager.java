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

import java.util.HashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.roster.MutableRoster;
import org.apache.vysper.xmpp.modules.roster.Roster;

/**
 * manages rosters in memory (and if the application ends, they are lost)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MemoryRosterManager extends AbstractRosterManager {

    private Map<Entity, MutableRoster> rosterMap = new HashMap<Entity, MutableRoster>();

    @Override
    protected Roster addNewRosterInternal(Entity jid) {
        MutableRoster mutableRoster = new MutableRoster();
        rosterMap.put(jid.getBareJID(), (MutableRoster) mutableRoster);
        return mutableRoster;
    }

    @Override
    protected Roster retrieveRosterInternal(Entity bareJid) {
        if (!rosterMap.containsKey(bareJid))
            rosterMap.put(bareJid, new MutableRoster());
        return rosterMap.get(bareJid);
    }

}
