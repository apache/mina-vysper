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

import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * to send a mail through the handler and afterwards actually forwarding the result to the client
 * inbound stanzas can only be forwarded when the client is authenticated
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class InboundStanzaProtocolWorker extends AbstractStateAwareProtocolWorker {

    @Override
    public SessionState getHandledState() {
        return SessionState.AUTHENTICATED;
    }

    @Override
    protected boolean isProcessingOutboundStanzas() {
        return false; // this worker is delivering inbound only
    }

    @Override
    protected void writeResponse(SessionContext sessionContext, ResponseStanzaContainer responseStanzaContainer) {
        if (responseStanzaContainer != null && responseStanzaContainer.getResponseStanza() != null) {
            Stanza responseStanza = responseStanzaContainer.getResponseStanza();

            sessionContext.getResponseWriter().write(responseStanza);
            return;
        }
        // there was nothing to write
    }
}
