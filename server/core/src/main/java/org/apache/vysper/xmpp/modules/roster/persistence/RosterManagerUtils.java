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
package org.apache.vysper.xmpp.modules.roster.persistence;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;

/**
 */
public class RosterManagerUtils {

    /**
     * retrieves the roster manager from the server runtime context and throws exception if not present
     *
     * @param serverRuntimeContext
     * @param sessionContext
     * @return roster manager - will not be NULL
     * @throws RuntimeException iff roster manager cannot be retrieved
     */
    public static RosterManager getRosterInstance(ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        RosterManager rosterManager;
        try {
            rosterManager = serverRuntimeContext.getStorageProvider(RosterManager.class);
        } catch (Exception e) {
            // System.err.println("failed to retrieve roster manager for session id = " + sessionContext.getSessionId());
            String sessionId = sessionContext == null ? "NO_SESSION" : sessionContext.getSessionId();
            throw new RuntimeException("failed to retrieve roster manager for session id = " + sessionId);
        }
        return rosterManager;
    }
}
