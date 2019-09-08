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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.Message;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchive;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageFilter;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessagePageRequest;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchive;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchivePreferences;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.user.SimpleUserMessageArchivePreferences;

/**
 * @author Réda Housni Alaoui
 */
public class InMemoryUserMessageArchive implements UserMessageArchive {

    private final MessageArchive delegate;

    private UserMessageArchivePreferences preferences;

    public InMemoryUserMessageArchive(Entity archiveId) {
        this.delegate = new InMemoryMessageArchive(archiveId);
        this.preferences = new SimpleUserMessageArchivePreferences();
    }

    @Override
    public ArchivedMessage archive(Message message) {
        return delegate.archive(message);
    }

    @Override
    public ArchivedMessages fetchSortedByOldestFirst(MessageFilter messageFilter, MessagePageRequest pageRequest) {
        return delegate.fetchSortedByOldestFirst(messageFilter, pageRequest);
    }

    @Override
    public ArchivedMessages fetchLastPageSortedByOldestFirst(MessageFilter messageFilter, long pageSize) {
        return delegate.fetchLastPageSortedByOldestFirst(messageFilter, pageSize);
    }

    @Override
    public UserMessageArchivePreferences preferences() {
        return preferences;
    }

    @Override
    public void changePreferences(UserMessageArchivePreferences preferences) {
        this.preferences = preferences;
    }
}
