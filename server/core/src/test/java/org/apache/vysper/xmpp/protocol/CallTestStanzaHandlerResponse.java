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

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 */
public class CallTestStanzaHandlerResponse extends CallTestStanzaHandler {
    private Stanza response;

    public CallTestStanzaHandlerResponse(String name) {
        super(name);
    }

    @Override
    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker)
            throws ProtocolException {
        super.execute(stanza, serverRuntimeContext, true, sessionContext, null, stanzaBroker);
        getResponseStanzas().forEach(stanzaBroker::writeToSession);
    }

    public void setResponseStanza(Stanza response) {
        this.response = response;
    }

    public List<Stanza> getResponseStanzas() {
        return Collections.singletonList(response);
    }

    public Stanza getUniqueResponseStanza() {
        return response;
    }

    public boolean hasResponse() {
        return response != null;
    }

}
