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
package org.apache.vysper.xmpp.protocol.worker;

import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EndOrClosedProtocolWorker extends AbstractStateAwareProtocolWorker {

    public EndOrClosedProtocolWorker(StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory) {
        super(stanzaHandlerExecutorFactory);
    }

    @Override
    public SessionState getHandledState() {
        return SessionState.ENDED;
    }

    @Override
    protected boolean checkState(InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza stanza,
								 StanzaHandler stanzaHandler) {
        throw new RuntimeException("session was terminated");
    }
}
