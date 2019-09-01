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

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * While stanzas hold the raw information read from stream, its handler holds
 * the logic for interpreting its semantics and execution. If stanzas are
 * commands, a StanzaHandler is a command processor. It is very much comparable
 * to a Servlet. StanzaHandler implementations must be stateless!
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface StanzaHandler {

    /**
     * the stanza name handled by this handler
     */
    String getName();

    /**
     * Allows to check the type of the handler by maintaining compatibility with
     * delegating handlers. Delegating handlers are expected to delegate this call
     * to their delegates.
     * 
     * @return The type wrapped by this handler
     */
    default Class<?> unwrapType() {
        return getClass();
    }

    /**
     * verifies if the stanza is processed by this handler
     * 
     * @return true, if it is processed, false otherwise
     */
    boolean verify(Stanza stanza);

    /**
     * specifies if a session context is needed for this handler
     */
    boolean isSessionRequired();

    /**
     * executes a stanza
     * 
     * @param isOutboundStanza
     *            true, if the stanza was emitted by the client which is handled by
     *            the session belonging to the given sessionContext parameter.
     *            false, if the session is receiving the stanza targeted to the
     *            session's client.
     */
    void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker)
            throws ProtocolException;
}
