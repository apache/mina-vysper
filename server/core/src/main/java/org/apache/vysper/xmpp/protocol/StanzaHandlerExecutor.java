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

import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * The stanza handler executor sole purpose is to execute a
 * {@link StanzaHandler}. Consumers must use this executor instead of directly
 * calling the
 * {@link StanzaHandler#execute(Stanza, ServerRuntimeContext, boolean, SessionContext, SessionStateHolder, StanzaBroker)}
 * 
 * An executor allows to decorate the {@link StanzaHandler} execution. For
 * example vysper could make sure that the executor always runs some process
 * before and/or after
 * {@link StanzaHandler#execute(Stanza, ServerRuntimeContext, boolean, SessionContext, SessionStateHolder, StanzaBroker)}.
 * 
 * @author Réda Housni Alaoui
 */
public interface StanzaHandlerExecutor {
    void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder) throws ProtocolException;
}
