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
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * this handler is aware of the session state and can act accordingly. for every
 * session state, a dedicated implementation exists. the most interesting case
 * is "authenticated", where the "common" XMPP happens (iq, message, presence)
 * special treatment has to be given to other states, which cover session
 * handshake and shutdown.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface StateAwareProtocolWorker {

    void processStanza(ServerRuntimeContext serverRuntimeContext, InternalSessionContext sessionContext,
            SessionStateHolder sessionStateHolder, Stanza stanza, StanzaHandler stanzaHandler);
}
