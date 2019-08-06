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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management.Set;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author Réda Housni Alaoui
 */
public class MatchingArchivedMessageResults {

    private final Entity initiatingEntity;

    private final Entity archiveId;

    private final Query query;

    private final ArchivedMessages archivedMessages;

    public MatchingArchivedMessageResults(Entity initiatingEntity, Entity archiveId, Query query,
            ArchivedMessages archivedMessages) {
        this.initiatingEntity = requireNonNull(initiatingEntity);
        this.archiveId = requireNonNull(archiveId);
        this.query = requireNonNull(query);
        this.archivedMessages = requireNonNull(archivedMessages);
    }

    public List<Stanza> toStanzas() {
        List<Stanza> stanzas = new ArrayList<>();
        archivedMessages.list().stream().map(archivedMessage -> new MatchingArchivedMessageResult(initiatingEntity,
                archiveId, query, archivedMessage)).map(MatchingArchivedMessageResult::toStanza).forEach(stanzas::add);
        stanzas.add(buildResultIq());
        return stanzas;
    }

    private Stanza buildResultIq() {
        Set set = buildSet();
        List<Attribute> finAttributes = new ArrayList<>();
        if (archivedMessages.isComplete()) {
            finAttributes.add(new Attribute("complete", "true"));
        }
        XMLElement fin = new XMLElement(query.getNamespace(), "fin", null, finAttributes,
                Collections.singletonList(set.element()));
        return StanzaBuilder.createDirectReply(query.iqStanza(), false, IQStanzaType.RESULT).addPreparedElement(fin)
                .build();
    }

    private Set buildSet() {
        if (archivedMessages.isEmpty()) {
            return Set.builder().count(archivedMessages.totalNumberOfMessages().orElse(null)).build();
        }

        List<ArchivedMessage> messagesList = archivedMessages.list();

        ArchivedMessage firstMessage = messagesList.get(0);
        ArchivedMessage lastMessage = messagesList.get(messagesList.size() - 1);

        return Set.builder().startFirst().index(archivedMessages.firstMessageIndex().orElse(null))
                .value(firstMessage.id()).endFirst().last(lastMessage.id())
                .count(archivedMessages.totalNumberOfMessages().orElse(null)).build();
    }
}
