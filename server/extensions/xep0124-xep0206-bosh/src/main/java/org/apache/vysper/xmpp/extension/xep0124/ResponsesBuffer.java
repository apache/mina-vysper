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
package org.apache.vysper.xmpp.extension.xep0124;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 */
public class ResponsesBuffer {

    protected static final int CAPACITY = 30;

    private static class Entry {
        Long rid; 
        BoshResponse response;
    }
    
    private final Queue<Entry> sentResponsesBacklog = new ArrayBlockingQueue<Entry>(CAPACITY);

    public void addAll(Map<Long, BoshResponse> responses) {
        for (Map.Entry<Long, BoshResponse> mapEntry : responses.entrySet()) {
            final Entry entry = new Entry();
            entry.response = mapEntry.getValue();
            entry.rid = mapEntry.getKey();
            putEntry(entry);
        }
    }
    
    public void add(Long rid, BoshResponse responses) {
        final Entry entry = new Entry();
        entry.response = responses;
        entry.rid = rid;
        putEntry(entry);
    }

    private void putEntry(Entry entry) {
        if (sentResponsesBacklog.size() == CAPACITY) sentResponsesBacklog.poll();
        sentResponsesBacklog.add(entry);
    }

    public BoshResponse lookup(Long rid) {
        for (Entry entry : sentResponsesBacklog) {
            if (entry.rid.equals(rid)) {
                return entry.response;
            }
        }
        return null;
    }
}
