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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchive;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchivePreferences;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 * @author Réda Housni Alaoui
 */
public class MAMIQPreferenceHandler extends DefaultIQHandler {

    private final String namespace;

    public MAMIQPreferenceHandler(String namespace) {
        this.namespace = namespace;
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, UserMessageArchivePreferencesElement.NAME)
                && verifyInnerNamespace(stanza, namespace);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity archiveId = sessionContext.getInitiatingEntity().getBareJID();

        MessageArchives archives = requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);

        Optional<UserMessageArchive> optionalArchive = archives.retrieveUserMessageArchive(archiveId);
        if (!optionalArchive.isPresent()) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND,
                    stanza, StanzaErrorType.CANCEL, "No user message archive found for entity " + archiveId, null,
                    null));
        }

        UserMessageArchive archive = optionalArchive.get();
        UserMessageArchivePreferences preferences = archive.preferences();

        XMLElement prefsElement = UserMessageArchivePreferencesElement.toXml(namespace, preferences);

        Stanza reply = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT)
                .addPreparedElement(prefsElement).build();
        return Collections.singletonList(reply);
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity archiveId = sessionContext.getInitiatingEntity().getBareJID();

        MessageArchives archives = requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);

        Optional<UserMessageArchive> optionalArchive = archives.retrieveUserMessageArchive(archiveId);
        if (!optionalArchive.isPresent()) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND,
                    stanza, StanzaErrorType.CANCEL, "No user message archive found for entity " + archiveId, null,
                    null));
        }

        UserMessageArchive archive = optionalArchive.get();

        UserMessageArchivePreferences preferences;
        try {
            XMLElement prefsElement = stanza.getSingleInnerElementsNamed(UserMessageArchivePreferencesElement.NAME);
            preferences = UserMessageArchivePreferencesElement.fromXml(prefsElement);
        } catch (XMLSemanticError xmlSemanticError) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST,
                    stanza, StanzaErrorType.MODIFY, null, null, null));
        }

        archive.changePreferences(preferences);

        XMLElement effectivePrefsElement = UserMessageArchivePreferencesElement.toXml(namespace, archive.preferences());
        Stanza reply = StanzaBuilder.createDirectReply(stanza, false, IQStanzaType.RESULT)
                .addPreparedElement(effectivePrefsElement).build();
        return Collections.singletonList(reply);
    }
}