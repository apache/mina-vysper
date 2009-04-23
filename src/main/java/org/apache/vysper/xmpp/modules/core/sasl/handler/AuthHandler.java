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
package org.apache.vysper.xmpp.modules.core.sasl.handler;

import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

import java.util.List;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class AuthHandler extends AbstractSASLHandler {
    public String getName() {
        return "auth";
    }

    public boolean isSessionRequired() {
        return true;
    }

    @Override
    protected ResponseStanzaContainer executeWorker(Stanza stanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        String requestedMechanism = stanza.getAttributeValue("mechanism");
        if (requestedMechanism == null) {
            return respondSASLFailure();
        }

        SASLMechanism identifiedMechanism = null;

        List<SASLMechanism> list = sessionContext.getServerRuntimeContext().getServerFeatures().getAuthenticationMethods();
        for (SASLMechanism saslMechanism : list) {
            if (saslMechanism.getName().equals(requestedMechanism)) {
                identifiedMechanism = saslMechanism;
                break;
            }
        }
        if (identifiedMechanism == null) throw new RuntimeException("return error");

        Stanza responseStanza = identifiedMechanism.started(sessionContext, sessionStateHolder, stanza);
        if (sessionStateHolder.getState() == SessionState.AUTHENTICATED) {
            AuthorizationRetriesCounter.removeFromSession(sessionContext);
        } else {
            AuthorizationRetriesCounter.getFromSession(sessionContext).countFailedTry();
        }

        return new ResponseStanzaContainerImpl(responseStanza);
    }

}
