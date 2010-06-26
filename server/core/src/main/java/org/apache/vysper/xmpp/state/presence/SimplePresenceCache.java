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
package org.apache.vysper.xmpp.state.presence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.PresenceStanza;

/**
 * unbounded in-memory-only cache, but entries are timestamped and oldest entry is the first in list
 */
public class SimplePresenceCache extends AbstractBaseCache {

    private final Map<Entity, Entry> presenceMap = new LinkedHashMap<Entity, Entry>();

    @Override
    protected void put0(Entity entity, PresenceStanza presenceStanza) {
        checkEntry(entity);
        // force adding at the end, this guarantees that the entry is the latest in getForBareJID()
        presenceMap.remove(entity);
        presenceMap.put(entity, new Entry(presenceStanza));
    }

    @Override
    protected PresenceStanza get0(Entity entity) throws PresenceCachingException {
        checkEntry(entity);
        Entry entry = presenceMap.get(entity);
        if (entry == null)
            return null;
        return entry.getPresenceStanza();
    }

    public PresenceStanza getForBareJID(Entity entity) throws PresenceCachingException {
        // TODO this is naive and not optimized. the whole key set is traversed every time
        PresenceStanza latest = null;
        for (Entity key : presenceMap.keySet()) {
            if (key.getBareJID().equals(entity)) {
                latest = presenceMap.get(key).getPresenceStanza(); // this is the latest until we find a newer one
            }
        }
        return latest;
    }

    public void remove(Entity entity) {
        presenceMap.remove(entity);
    }

    static class Entry {
        protected long timestamp = System.currentTimeMillis();

        protected PresenceStanza presenceStanza;

        Entry(PresenceStanza presenceStanza) {
            this.presenceStanza = presenceStanza;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public PresenceStanza getPresenceStanza() {
            return presenceStanza;
        }
    }
}
