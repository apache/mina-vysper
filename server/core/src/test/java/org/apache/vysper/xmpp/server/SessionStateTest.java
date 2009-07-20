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
package org.apache.vysper.xmpp.server;

import junit.framework.TestCase;

/**
 */
public class SessionStateTest extends TestCase {

    public void testCompare() {
        assertEquals("diff between first and last state", -6, SessionState.INITIATED.compareTo(SessionState.CLOSED));

        assertEquals("diff to next state 1", -1, SessionState.INITIATED.compareTo(SessionState.STARTED));
        assertEquals("diff to next state 2", -1, SessionState.STARTED.compareTo(SessionState.ENCRYPTION_STARTED));
        assertEquals("diff to next state 3", -1, SessionState.ENCRYPTION_STARTED.compareTo(SessionState.ENCRYPTED));
        assertEquals("diff to next state 4", -1, SessionState.ENCRYPTED.compareTo(SessionState.AUTHENTICATED));
        assertEquals("diff to next state 5", -1, SessionState.AUTHENTICATED.compareTo(SessionState.ENDED));
        assertEquals("diff to next state 6", -1, SessionState.ENDED.compareTo(SessionState.CLOSED));
    }

}
