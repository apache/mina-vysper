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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.in_memory;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.Message;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchive;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageFilter;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessagePageRequest;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.SimpleArchivedMessage;

/**
 * @author Réda Housni Alaoui
 */
public class InMemoryMessageArchive implements MessageArchive {

    private final Entity archiveId;

    private final List<SimpleArchivedMessage> messages = new ArrayList<>();

    public InMemoryMessageArchive(Entity archiveId) {
        this.archiveId = requireNonNull(archiveId);
    }

    @Override
    public ArchivedMessage archive(Message message) {
        SimpleArchivedMessage archivedMessage = new SimpleArchivedMessage(UUID.randomUUID().toString(), message);
        messages.add(archivedMessage);
        return archivedMessage;
    }

    @Override
    public ArchivedMessages fetchSortedByOldestFirst(MessageFilter messageFilter, MessagePageRequest pageRequest) {
        List<ArchivedMessage> filteredMessages = filterMessages(new InMemoryMessageFilter(messageFilter));
        return new InMemoryArchivedMessagesPage(pageRequest, filteredMessages);
    }

    @Override
    public ArchivedMessages fetchLastPageSortedByOldestFirst(MessageFilter messageFilter, long pageSize) {
        List<ArchivedMessage> filteredMessages = filterMessages(new InMemoryMessageFilter(messageFilter));
        return new InMemoryArchivedMessagesLastPage(pageSize, filteredMessages);
    }

    private List<ArchivedMessage> filterMessages(Predicate<ArchivedMessage> predicate) {
        return messages.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InMemoryMessageArchive.class.getSimpleName() + "[", "]")
                .add("archiveId=" + archiveId).toString();
    }
}
