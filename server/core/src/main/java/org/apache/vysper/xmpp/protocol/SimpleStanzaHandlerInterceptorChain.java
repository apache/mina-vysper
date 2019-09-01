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
package org.apache.vysper.xmpp.protocol;

import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Réda Housni Alaoui
 */
class SimpleStanzaHandlerInterceptorChain implements StanzaHandlerInterceptorChain {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleStanzaHandlerInterceptorChain.class);

    private final StanzaHandler stanzaHandler;

    private final Queue<StanzaHandlerInterceptor> interceptors;

    public SimpleStanzaHandlerInterceptorChain(StanzaHandler stanzaHandler,
            List<StanzaHandlerInterceptor> interceptors) {
        this.stanzaHandler = requireNonNull(stanzaHandler);
        this.interceptors = new LinkedList<>(interceptors);
    }

    @Override
    public void intercept(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker)
            throws ProtocolException {
        StanzaHandlerInterceptor interceptor = interceptors.poll();
        if (interceptor == null) {
            LOG.debug("No more interceptor to execute. Executing stanza handler {}.", stanzaHandler);
            stanzaHandler.execute(stanza, serverRuntimeContext, isOutboundStanza, sessionContext, sessionStateHolder,
                    stanzaBroker);
            return;
        }
        LOG.debug("Executing interceptor {}", interceptor);
        interceptor.intercept(stanza, serverRuntimeContext, isOutboundStanza, sessionContext, sessionStateHolder,
                stanzaBroker, this);
    }
}
