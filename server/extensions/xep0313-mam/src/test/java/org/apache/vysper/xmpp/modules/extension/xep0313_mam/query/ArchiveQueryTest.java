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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchive;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class ArchiveQueryTest {

    private static final Entity JULIET = EntityImpl.parseUnchecked("juliet@foo.com");

    private static final Entity ARCHIVE_ID = JULIET;

    private MessageArchive archive;

    private X x;

    private QuerySet querySet;

    private ArchiveQuery tested;

    @Before
    public void before() throws XMLSemanticError {
        archive = mock(MessageArchive.class);
        Query query = mock(Query.class);
        x = mock(X.class);
        when(query.getX()).thenReturn(x);
        querySet = mock(QuerySet.class);
        when(query.getSet()).thenReturn(querySet);
        tested = new ArchiveQuery(archive, ARCHIVE_ID, query);
    }

    @Test
    public void executeWithoutWith() throws XMLSemanticError {
        when(x.getWith()).thenReturn(Optional.empty());

        ArchivedMessages archivedMessages = mock(ArchivedMessages.class);
        when(archive.fetchSortedByOldestFirst(any(), any())).thenReturn(archivedMessages);

        assertEquals(archivedMessages, tested.execute());
    }

    @Test
    public void executeWithWith() throws XMLSemanticError {
        when(x.getWith()).thenReturn(Optional.of(JULIET));

        ArchivedMessages archivedMessages = mock(ArchivedMessages.class);
        when(archive.fetchSortedByOldestFirst(any(), any())).thenReturn(archivedMessages);

        assertEquals(archivedMessages, tested.execute());
    }

}