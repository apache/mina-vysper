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

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.muc.MUCArchiveQueryHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.pubsub.PubsubNodeArchiveQueryHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.user.UserArchiveQueryHandler;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 * @author Réda Housni Alaoui
 */
public class MAMIQQueryHandler extends DefaultIQHandler {

    private final String namespace;

    private final List<QueryHandler> queryHandlers;

    public MAMIQQueryHandler(String namespace) {
        this(namespace, new PubsubNodeArchiveQueryHandler(), new MUCArchiveQueryHandler(),
                new UserArchiveQueryHandler());
    }

    public MAMIQQueryHandler(String namespace, QueryHandler pubsubNodeArchiveQueryHandler,
                             QueryHandler mucArchiveQueryHandler, QueryHandler userArchiveQueryHandler) {
        this.namespace = requireNonNull(namespace);
        List<QueryHandler> modifiableQueryHandlers = new ArrayList<>();
        modifiableQueryHandlers.add(pubsubNodeArchiveQueryHandler);
        modifiableQueryHandlers.add(mucArchiveQueryHandler);
        modifiableQueryHandlers.add(userArchiveQueryHandler);
        this.queryHandlers = Collections.unmodifiableList(modifiableQueryHandlers);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, Query.ELEMENT_NAME) && verifyInnerNamespace(stanza, namespace);
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, StanzaBroker broker) {
        Query query;
        try {
            query = new Query(namespace, stanza);
        } catch (XMLSemanticError xmlSemanticError) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE,
                    stanza, StanzaErrorType.CANCEL, null, null, null));
        }

        return queryHandlers.stream().filter(handler -> handler.supports(query, serverRuntimeContext, sessionContext))
                .map(handler -> handler.handle(query, serverRuntimeContext, sessionContext)).findFirst()
                .orElseGet(() -> Collections.singletonList(ServerErrorResponses.getStanzaError(
                        StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.CANCEL, null, null, null)));
    }

}
