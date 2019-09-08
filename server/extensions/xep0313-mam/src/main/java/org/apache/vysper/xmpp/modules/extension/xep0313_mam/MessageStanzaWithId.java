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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author Réda Housni Alaoui
 */
public class MessageStanzaWithId {

    private static final String STANZA_ID = "stanza-id";

    private final ArchivedMessage archivedMessage;

    private final Entity archiveId;

    public MessageStanzaWithId(ArchivedMessage archivedMessage, Entity archiveId) {
        this.archivedMessage = requireNonNull(archivedMessage);
        this.archiveId = requireNonNull(archiveId);
    }

    public Stanza toStanza() {
        MessageStanza archivedMessageStanza = archivedMessage.stanza();

        List<XMLElement> innerElements = new ArrayList<>();
        archivedMessageStanza.getInnerElements().stream().filter(notStanzaId()).forEach(innerElements::add);
        List<Attribute> stanzaIdAttributes = new ArrayList<>();
        stanzaIdAttributes.add(new Attribute("by", archiveId.getFullQualifiedName()));
        stanzaIdAttributes.add(new Attribute("id", archivedMessage.id()));
        innerElements.add(new XMLElement(NamespaceURIs.XEP0359_STANZA_IDS, STANZA_ID, null, stanzaIdAttributes,
                Collections.emptyList()));

        StanzaBuilder archivedMessageStanzaWithIdBuilder = StanzaBuilder.createClone(archivedMessageStanza, false,
                Collections.emptyList());
        innerElements.forEach(archivedMessageStanzaWithIdBuilder::addPreparedElement);

        return archivedMessageStanzaWithIdBuilder.build();
    }

    private Predicate<XMLElement> notStanzaId() {
        return xmlElement -> !STANZA_ID.equals(xmlElement.getName())
                || !NamespaceURIs.XEP0359_STANZA_IDS.equals(xmlElement.getNamespaceURI());
    }

}
