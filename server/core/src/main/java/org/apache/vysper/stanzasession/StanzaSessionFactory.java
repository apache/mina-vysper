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
package org.apache.vysper.stanzasession;

import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.InternalServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaSessionFactory implements Endpoint {

    private ServerRuntimeContext serverRuntimeContext;
    private StanzaProcessor stanzaProcessor;

    /**
     * returns a new session for the server. the session behaves like a client, but lives within the server JVM
     */
    public StanzaSession createNewSession() {
        SessionStateHolder stateHolder = new SessionStateHolder();
        stateHolder.setState(SessionState.INITIATED);
        StanzaSessionContext sessionContext = new StanzaSessionContext(serverRuntimeContext, stanzaProcessor, stateHolder);
        StanzaSession session = new StanzaSession(sessionContext);
        return session;
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    @Override
    public void setStanzaProcessor(StanzaProcessor stanzaProcessor) {
        this.stanzaProcessor = stanzaProcessor;
    }

    public void start() {
        // nothing to do
    }

    public void stop() {
        // nothing to do
    }
}
