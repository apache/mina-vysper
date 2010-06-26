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

/**
 * all states for the session state machine. states are visited sequential
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum SessionState {

    /**
     *
     */
    UNCONNECTED,

    /**
     * stream opened but no start stanza processed
     */
    INITIATED,

    /**
     * plain start stanza processed, but not secured
     */
    STARTED,

    /**
     * STARTTLS processed, but not finished
     */
    ENCRYPTION_STARTED,

    /**
     * secure start stanza processed, but not authenticated
     */
    ENCRYPTED,

    /**
     * authentication has begun, but is not yet finished
     */
    //    AUTHENTICATION_STARTED,

    /**
     * authenticated
     */
    AUTHENTICATED,

    /**
     * a resource has been bound to the server
     */
    //    RESOURCE_BOUND,

    /**
     * the initial session handshake is completed and regular stanzas are processed
     */
    //    OPEN_FOR_XMPP,

    /**
     * end stanza processed
     */
    ENDED,

    /**
     * underlying stream closed
     */
    CLOSED
}
