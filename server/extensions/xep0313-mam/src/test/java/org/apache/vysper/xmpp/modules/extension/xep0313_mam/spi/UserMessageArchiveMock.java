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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.apache.vysper.xmpp.stanza.MessageStanza;

/**
 * @author Réda Housni Alaoui
 */
public class UserMessageArchiveMock implements UserMessageArchive {

    private final Queue<Message> messages = new LinkedList<>();

    @Override
    public ArchivedMessage archive(Message message) {
        messages.add(message);
        return new SimpleArchivedMessage(UUID.randomUUID().toString(), message);
    }

    @Override
    public ArchivedMessages fetchSortedByOldestFirst(MessageFilter messageFilter, MessagePageRequest pageRequest) {
        return new SimpleArchivedMessages(Collections.emptyList(), 0L, 0L);
    }

    @Override
    public ArchivedMessages fetchLastPageSortedByOldestFirst(MessageFilter messageFilter, long pageSize) {
        return new SimpleArchivedMessages(Collections.emptyList(), 0L, 0L);
    }

    public void clear() {
        messages.clear();
    }

    public void assertUniqueArchivedMessageStanza(MessageStanza messageStanza) {
        assertEquals(1, messages.size());
        assertEquals(messageStanza, messages.peek().stanza());
    }

    public void assertEmpty() {
        assertTrue(messages.isEmpty());
    }

    @Override
    public UserMessageArchivePreferences preferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePreferences(UserMessageArchivePreferences preferences) {
        throw new UnsupportedOperationException();
    }
}
