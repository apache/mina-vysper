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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.ArchiveQuery;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.MatchingArchivedMessageResults;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.Query;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.QueryHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchive;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 * @author Réda Housni Alaoui
 */
public class UserArchiveQueryHandler implements QueryHandler {

    @Override
    public boolean supports(Query query, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        return true;
    }

    @Override
    public List<Stanza> handle(Query query, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        try {
            return doHandle(query, serverRuntimeContext, sessionContext);
        } catch (XMLSemanticError xmlSemanticError) {
            Stanza internalServerError = ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR,
                    query.iqStanza(), StanzaErrorType.CANCEL, null, null, null);
            return Collections.singletonList(internalServerError);
        }
    }

    private List<Stanza> doHandle(Query query, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext)
            throws XMLSemanticError {
        Entity initiatingEntity = sessionContext.getInitiatingEntity();
        Entity archiveId = ofNullable(query.iqStanza().getTo()).orElse(initiatingEntity).getBareJID();

        if (!sessionContext.getInitiatingEntity().getBareJID().equals(archiveId)) {
            // The initiating user is trying to read the archive of another user
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN,
                    query.iqStanza(), StanzaErrorType.CANCEL,
                    "Entity " + initiatingEntity + " is not allowed to query " + archiveId + " message archives.", null,
                    null));
        }

        MessageArchives archives = requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);

        Optional<UserMessageArchive> archive = archives.retrieveUserMessageArchive(archiveId);
        if (!archive.isPresent()) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND,
                    query.iqStanza(), StanzaErrorType.CANCEL, "No user message archive found for entity " + archiveId,
                    null, null));
        }

        ArchivedMessages archivedMessages = new ArchiveQuery(archive.get(), archiveId, query).execute();
        return new MatchingArchivedMessageResults(initiatingEntity, archiveId, query, archivedMessages).toStanzas();
    }
}
