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

import org.apache.vysper.xmpp.modules.core.starttls.handler.StartTLSHandler;
import org.apache.vysper.xmpp.modules.extension.xep007_inbandreg.InBandRegistrationHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbResultHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbVerifyHandler;
import org.apache.vysper.xmpp.protocol.ResponseWriter;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StartedProtocolWorker extends AbstractStateAwareProtocolWorker {

    @Override
    public SessionState getHandledState() {
        return SessionState.STARTED;
    }

    @Override
    protected boolean checkState(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza stanza,
            StanzaHandler stanzaHandler) {

        if (stanzaHandler instanceof StartTLSHandler) {
            return true;
        } else if (stanzaHandler instanceof InBandRegistrationHandler) {
            return true;
        } else if (sessionContext.isServerToServer() && stanzaHandler instanceof DbVerifyHandler) {
            return true;
        } else if (sessionContext.isServerToServer() && stanzaHandler instanceof DbResultHandler) {
            return true;
        }
        ResponseWriter.writeUnsupportedStanzaError(sessionContext);
        return false;
    }
}