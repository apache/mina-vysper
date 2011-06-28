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
package org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryOfflineStorageProvider extends AbstractOfflineStorageProvider {

    final Logger logger = LoggerFactory.getLogger(MemoryOfflineStorageProvider.class);

    private long timeout;

    private Map<String, List<Entry>> offlineStorageMap = new HashMap<String, List<Entry>>();

    public MemoryOfflineStorageProvider() {
        this(7 * 24 * 3600 * 1000); // default to seven days;
    }

    public MemoryOfflineStorageProvider(long timeout) {
        int delay = 60 * 100 * 1000;
        int period = 60 * 100 * 1000;

        this.timeout = timeout;
        Timer timer = new Timer("OfflineTimeoutCheckerTimer", true);
        timer.schedule(new TimeoutChecker(), delay, period);
    }

    @Override
    protected void storeStanza(Stanza stanza) {
        Entity to = stanza.getTo();
        String bareJID = to.getBareJID().getFullQualifiedName();
        synchronized (offlineStorageMap) {
            List<Entry> entriesForJID = offlineStorageMap.get(bareJID);
            if (entriesForJID == null) {
                entriesForJID = new ArrayList<Entry>();
                offlineStorageMap.put(bareJID, entriesForJID);
            }
            entriesForJID.add(new Entry(stanza, new Date().getTime()));
        }
    }

    public Collection<Stanza> getStanzasFor(Entity jid) {
        synchronized (offlineStorageMap) {
            List<Entry> entries = offlineStorageMap.remove(jid.getBareJID().getFullQualifiedName());
            if (entries == null) {
                return Collections.emptyList();
            } else {

                List<Stanza> stanzas = new ArrayList<Stanza>();
                for (Entry entry : entries) {
                    // TODO add timestamp to messages
                    stanzas.add(entry.getStanza());
                }
                return stanzas;
            }
        }
    }

    private static class Entry {

        private Stanza stanza;

        public Entry(Stanza stanza, long timeStamp) {
            super();
            this.stanza = stanza;
            this.timeStamp = timeStamp;
        }

        private long timeStamp;

        public long getTimeStamp() {
            return timeStamp;
        }

        public Stanza getStanza() {
            return stanza;
        }

    }

    private class TimeoutChecker extends TimerTask {
        public void run() {
            logger.debug("Running timeout checker for offline stanzas");
            long timestamp = new Date().getTime() - timeout;
            Set<String> jids = offlineStorageMap.keySet();
            for (String jid : jids) {
                synchronized (offlineStorageMap) {
                    List<Entry> entries = offlineStorageMap.get(jid);
                    if (entries != null) {
                        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
                            Entry entry = it.next();
                            if (entry.getTimeStamp() < timestamp) {
                                logger.debug("Removed timed out offline stanza");
                                it.remove();
                            }
                        }
                    }
                }
            }

        }
    }

}
