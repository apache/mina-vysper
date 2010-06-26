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
package org.apache.vysper.xmpp.modules.core.sasl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.vysper.xmpp.server.SessionContext;

/**
 * used to count authentication retries.
 * after the count is down to zero
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AuthorizationRetriesCounter {

    public static final String SESSION_ATTRIBUTE_ABORTION_COUNTER = "authorizationRetriesCounter";

    public static AuthorizationRetriesCounter getFromSession(SessionContext sessionContext) {
        AuthorizationRetriesCounter counter = (AuthorizationRetriesCounter) sessionContext
                .getAttribute(SESSION_ATTRIBUTE_ABORTION_COUNTER);

        synchronized (sessionContext) {
            if (counter == null) {
                int retries = sessionContext.getServerRuntimeContext().getServerFeatures().getAuthenticationRetries();
                counter = new AuthorizationRetriesCounter(retries);
                sessionContext.putAttribute(SESSION_ATTRIBUTE_ABORTION_COUNTER, counter);
            }
        }
        return counter;
    }

    public static void removeFromSession(SessionContext sessionContext) {
        sessionContext.putAttribute(SESSION_ATTRIBUTE_ABORTION_COUNTER, null);
    }

    AtomicInteger counter;

    public AuthorizationRetriesCounter(int counter) {
        if (counter <= 0)
            throw new IllegalArgumentException("counter must be positive");
        this.counter = new AtomicInteger(counter);
    }

    public boolean hasTriesLeft() {
        return counter.intValue() > 0;
    }

    /**
     *
     * @return TRUE has tries left, FALSE has no tries left, should lead to session termination
     */
    public boolean countFailedTry() {
        if (!hasTriesLeft())
            return false;
        return counter.decrementAndGet() > 0;
    }

    public int getTriesLeft() {
        return counter.intValue();
    }
}
