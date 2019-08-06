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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessagePageRequest;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.SimpleMessagePageRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class InMemoryPageLimitedArchivedMessagesTest {

    private static final String MESSAGE_1_ID = "message-1";

    private static final String MESSAGE_2_ID = "message-2";

    private static final String MESSAGE_3_ID = "message-3";

    private ArchivedMessage message1;

    private ArchivedMessage message2;

    private ArchivedMessage message3;

    private List<ArchivedMessage> messages;

    @Before
    public void before() {
        message1 = mock(ArchivedMessage.class);
        when(message1.id()).thenReturn(MESSAGE_1_ID);
        message2 = mock(ArchivedMessage.class);
        when(message2.id()).thenReturn(MESSAGE_2_ID);
        message3 = mock(ArchivedMessage.class);
        when(message3.id()).thenReturn(MESSAGE_3_ID);

        messages = new ArrayList<>();
        messages.add(message1);
        messages.add(message2);
        messages.add(message3);
    }

    @Test
    public void withoutLimit() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(null, null, null);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit, messages);

        assertFalse(tested.isEmpty());
        assertTrue(tested.isComplete());
        assertEquals(3, (long) tested.totalNumberOfMessages().orElse(0L));
        assertEquals(0, (long) tested.firstMessageIndex().orElse(-1L));
        List<ArchivedMessage> list = tested.list();
        assertEquals(3, list.size());
        assertEquals(message1, list.get(0));
        assertEquals(message2, list.get(1));
        assertEquals(message3, list.get(2));
    }

    @Test
    public void withoutFirstOrLastMessageId() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(2L, null, null);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit, messages);

        assertFalse(tested.isEmpty());
        assertFalse(tested.isComplete());
        assertEquals(3, (long) tested.totalNumberOfMessages().orElse(0L));
        assertEquals(0, (long) tested.firstMessageIndex().orElse(-1L));
        List<ArchivedMessage> list = tested.list();
        assertEquals(2, list.size());
        assertEquals(message1, list.get(0));
        assertEquals(message2, list.get(1));
    }

    @Test
    public void withFirstMessageId() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(2L, MESSAGE_2_ID, null);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit, messages);

        assertFalse(tested.isEmpty());
        assertFalse(tested.isComplete());
        assertEquals(3, (long) tested.totalNumberOfMessages().orElse(0L));
        assertEquals(2, (long) tested.firstMessageIndex().orElse(-1L));
        List<ArchivedMessage> list = tested.list();
        assertEquals(1, list.size());
        assertEquals(message3, list.get(0));
    }

    @Test
    public void withLastMessageId() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(2L, null, MESSAGE_2_ID);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit, messages);

        assertFalse(tested.isEmpty());
        assertFalse(tested.isComplete());
        assertEquals(3, (long) tested.totalNumberOfMessages().orElse(0L));
        assertEquals(0, (long) tested.firstMessageIndex().orElse(-1L));
        List<ArchivedMessage> list = tested.list();
        assertEquals(1, list.size());
        assertEquals(message1, list.get(0));
    }

    @Test
    public void withFirstAndLastMessageId() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(2L, MESSAGE_1_ID, MESSAGE_3_ID);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit, messages);

        assertFalse(tested.isEmpty());
        assertFalse(tested.isComplete());
        assertEquals(3, (long) tested.totalNumberOfMessages().orElse(0L));
        assertEquals(1, (long) tested.firstMessageIndex().orElse(-1L));
        List<ArchivedMessage> list = tested.list();
        assertEquals(1, list.size());
        assertEquals(message2, list.get(0));
    }

    @Test
    public void empty() {
        MessagePageRequest pageLimit = new SimpleMessagePageRequest(2L, MESSAGE_1_ID, MESSAGE_3_ID);
        InMemoryArchivedMessagesPage tested = new InMemoryArchivedMessagesPage(pageLimit,
                Collections.emptyList());

        assertTrue(tested.isEmpty());
        assertTrue(tested.isComplete());
        assertEquals(0, (long) tested.totalNumberOfMessages().orElse(0L));
        assertNull(tested.firstMessageIndex().orElse(null));
        List<ArchivedMessage> list = tested.list();
        assertTrue(list.isEmpty());
    }

}