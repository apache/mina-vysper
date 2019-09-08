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

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.interceptor.MAMStanzaBrokerProvider;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
public class UserMessageStanzaBrokerProvider implements MAMStanzaBrokerProvider {
    @Override
    public boolean supports(Stanza processedStanza, ServerRuntimeContext serverRuntimeContext) {
        return processedStanza != null && MessageStanza.isOfType(processedStanza);
    }

    @Override
    public StanzaBroker proxy(StanzaBroker delegate, ServerRuntimeContext serverRuntimeContext,
                              SessionContext sessionContext, boolean isOutboundStanza, boolean forceArchiving) {
        return new UserMessageStanzaBroker(delegate, serverRuntimeContext, sessionContext,
				isOutboundStanza, forceArchiving);
    }
}
