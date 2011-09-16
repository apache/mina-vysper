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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * a mutable roster implementation, only holding items in memory
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MutableRoster implements Roster {
    private final Map<Entity, RosterItem> items = new LinkedHashMap<Entity, RosterItem>();

    public Iterator<RosterItem> iterator() {
        return items.values().iterator();
    }

    public RosterItem getEntry(Entity contact) {
        return items.get(contact);
    }

    public void addItem(RosterItem rosterItem) {
        if (rosterItem == null || rosterItem.getJid() == null)
            throw new RuntimeException("roster item and item's jid must not be null.");
        items.put(rosterItem.getJid().getBareJID(), rosterItem);
    }

    public boolean removeItem(Entity contact) {
        return items.remove(contact) != null;
    }
}
