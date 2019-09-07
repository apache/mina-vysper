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
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.datetime.DateTimeProfile;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.MessageStanzaWithId;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author Réda Housni Alaoui
 */
class MatchingArchivedMessageResult {

    private final Entity initiatingEntity;

    private final Entity archiveId;

    private final Query query;

    private final ArchivedMessage archivedMessage;

    MatchingArchivedMessageResult(Entity initiatingEntity, Entity archiveId, Query query,
            ArchivedMessage archivedMessage) {
        this.initiatingEntity = requireNonNull(initiatingEntity);
        this.archiveId = requireNonNull(archiveId);
        this.query = requireNonNull(query);
        this.archivedMessage = requireNonNull(archivedMessage);
    }

    Stanza toStanza() {
        XMLElement result = createResult();
        return new StanzaBuilder("message").addAttribute("to", initiatingEntity.getFullQualifiedName())
                .addPreparedElement(result).build();
    }

    private XMLElement createResult() {
        XMLElement forwarded = createForwarded();
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("id", archivedMessage.id()));
        query.getQueryId().map(queryId -> new Attribute("queryid", queryId)).ifPresent(attributes::add);
        return new XMLElement(query.getNamespace(), "result", null, attributes, Collections.singletonList(forwarded));
    }

    private XMLElement createForwarded() {
        Stanza archivedStanzaWithId = new MessageStanzaWithId(archivedMessage, archiveId).toStanza();

        String stamp = DateTimeProfile.getInstance().getDateTimeInUTC(archivedMessage.dateTime());

        List<XMLFragment> innerElements = new ArrayList<>();
        innerElements.add(new XMLElement(NamespaceURIs.URN_XMPP_DELAY, "delay", null,
                Collections.singletonList(new Attribute("stamp", stamp)), Collections.emptyList()));
        innerElements.add(archivedStanzaWithId);
        return new XMLElement(NamespaceURIs.XEP0297_STANZA_FORWARDING, "forwarded", null, Collections.emptyList(),
                innerElements);
    }

}
