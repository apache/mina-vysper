/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.vysper.xmpp.state.presence;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.PresenceStanza;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * unbounded in-memory-only cache, but entries are timestamped and oldest entry is the first in list
 */
public class SimplePresenceCache implements LatestPresenceCache {

    private final Map<Entity, Entry> presenceMap = new LinkedHashMap<Entity, Entry>();

    public void put(Entity entity, PresenceStanza presenceStanza) {
        checkEntry(entity);
        presenceMap.put(entity, new Entry(presenceStanza));
    }

    private void checkEntry(Entity entity) {
        if (entity == null) throw new PresenceCachingException("entity might not be null");
        if (entity.getResource() == null) throw new PresenceCachingException("presense stanzas are cached per resource, failure for " + entity.getFullQualifiedName());
    }

    public PresenceStanza get(Entity entity) throws PresenceCachingException {
        checkEntry(entity);
        Entry entry = presenceMap.get(entity);
        return entry.getPresenceStanza();
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
