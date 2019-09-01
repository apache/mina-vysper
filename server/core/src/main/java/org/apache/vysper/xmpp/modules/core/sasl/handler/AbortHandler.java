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

import org.apache.vysper.xmpp.modules.core.sasl.AuthorizationRetriesCounter;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AbortHandler extends AbstractSASLHandler {
    public String getName() {
        return "abort";
    }

    public boolean isSessionRequired() {
        return true;
    }

    @Override
    public Stanza executeWorker(Stanza stanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {

        AuthorizationRetriesCounter counter = AuthorizationRetriesCounter.getFromSession(sessionContext);
        boolean moreTriesLeft = counter.countFailedTry(); // record that client aborted

        // TODO do more clean-ups as mechanism requires.

        return new ServerResponses().getAuthAborted();
    }

}