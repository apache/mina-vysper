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

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchive;

/**
 * @author Réda Housni Alaoui
 */
public class ArchiveQuery {

    private final MessageArchive archive;

    private final Entity archiveId;

    private final Query query;

    public ArchiveQuery(MessageArchive archive, Entity archiveId, Query query) {
        this.archive = requireNonNull(archive);
        this.archiveId = requireNonNull(archiveId);
        this.query = requireNonNull(query);
    }

    public ArchivedMessages execute() throws XMLSemanticError {
        ArchiveFilter archiveFilter = new ArchiveFilter(archiveId, query.getX());
        QuerySet querySet = query.getSet();

        if (querySet.lastPage()) {
            long pageSize = querySet.pageSize().orElseThrow(
                    () -> new IllegalArgumentException("Page size must be defined when requesting last page"));
            return archive.fetchLastPageSortedByOldestFirst(archiveFilter, pageSize);
        } else {
            return archive.fetchSortedByOldestFirst(archiveFilter, querySet);
        }
    }

}
