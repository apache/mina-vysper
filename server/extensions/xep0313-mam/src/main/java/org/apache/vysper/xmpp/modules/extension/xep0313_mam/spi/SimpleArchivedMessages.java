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

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleArchivedMessages implements ArchivedMessages {

    private final List<ArchivedMessage> list;

    private final Long firstMessageIndex;

    private final Long totalNumberOfMessages;

    public SimpleArchivedMessages(List<ArchivedMessage> list) {
        this(list, null, null);
    }

    public SimpleArchivedMessages(List<ArchivedMessage> list, Long firstMessageIndex) {
        this(list, firstMessageIndex, null);
    }

    public SimpleArchivedMessages(List<ArchivedMessage> list, Long firstMessageIndex, Long totalNumberOfMessages) {
        this.list = new ArrayList<>(list);
        this.firstMessageIndex = firstMessageIndex;
        this.totalNumberOfMessages = totalNumberOfMessages;
    }

    public static SimpleArchivedMessages empty() {
        return new SimpleArchivedMessages(Collections.emptyList());
    }

    @Override
    public List<ArchivedMessage> list() {
        return new ArrayList<>(list);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean isComplete() {
        return list.size() == totalNumberOfMessages;
    }

    @Override
    public Optional<Long> firstMessageIndex() {
        return ofNullable(firstMessageIndex);
    }

    @Override
    public Optional<Long> totalNumberOfMessages() {
        return ofNullable(totalNumberOfMessages);
    }
}
