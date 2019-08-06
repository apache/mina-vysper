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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.pubsub;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.QueryHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.Query;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 * @author Réda Housni Alaoui
 */
public class PubsubNodeArchiveQueryHandler implements QueryHandler {

    @Override
    public boolean supports(Query query, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        return query.getNode().isPresent();
    }

    @Override
    public List<Stanza> handle(Query query, ServerRuntimeContext serverRuntimeContext,
                               SessionContext sessionContext) {
        // PubSub node archives is not yet implemented
        Stanza notImplemented = ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED,
                query.iqStanza(), StanzaErrorType.CANCEL, "Pubsub node message archive feature is not yet implemented",
                null, null);
        return Collections.singletonList(notImplemented);
    }
}
