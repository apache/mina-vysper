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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.muc.MUCMessageStanzaBrokerProvider;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.user.UserMessageStanzaBrokerProvider;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptorChain;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
public class MAMStanzaHandlerInterceptor implements StanzaHandlerInterceptor {

    private final List<MAMStanzaBrokerProvider> stanzaBrokerProviders;

    public MAMStanzaHandlerInterceptor() {
        // TODO add pubsub
        List<MAMStanzaBrokerProvider> modifiableStanzaBrokerProviders = new ArrayList<>();
        modifiableStanzaBrokerProviders.add(new MUCMessageStanzaBrokerProvider());
        modifiableStanzaBrokerProviders.add(new UserMessageStanzaBrokerProvider());
        this.stanzaBrokerProviders = Collections.unmodifiableList(modifiableStanzaBrokerProviders);
    }

    @Override
    public void intercept(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker,
            StanzaHandlerInterceptorChain interceptorChain) throws ProtocolException {

        StanzaBroker stanzaBrokerToUse = stanzaBrokerProviders.stream()
                .filter(mamStanzaBrokerProvider -> mamStanzaBrokerProvider.supports(stanza, serverRuntimeContext))
                .map(mamStanzaBrokerProvider -> mamStanzaBrokerProvider.proxy(stanzaBroker, serverRuntimeContext,
                        sessionContext, isOutboundStanza))
                .findFirst().orElse(stanzaBroker);

        interceptorChain.intercept(stanza, serverRuntimeContext, isOutboundStanza, sessionContext, sessionStateHolder,
                stanzaBrokerToUse);
    }
}
