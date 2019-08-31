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
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServiceUnavailableStanzaErrorHandler implements StanzaHandler {

    public String getName() {
        return "stanza_not_supported_error";
    }

    public boolean verify(Stanza stanza) {
        return true;
    }

    public boolean isSessionRequired() {
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {
        if (!(stanza instanceof XMPPCoreStanza)) {
            stanza = XMPPCoreStanza.getWrapper(stanza);
        }
        if (stanza == null)
            throw new IllegalArgumentException("cannot coerce into a message, iq or presence stanza");

        XMPPCoreStanza coreStanza = (XMPPCoreStanza) stanza;
        Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE, coreStanza,
                StanzaErrorType.CANCEL, "namespace not supported", null, null);
        stanzaBroker.writeToSession(errorStanza);
    }
}
