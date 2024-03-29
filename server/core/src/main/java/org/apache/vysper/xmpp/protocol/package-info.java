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

/**
 * The protocol layer is the central place for handling the fundamental logic and assertions of XMPP stanzas going over a XMPP transport.
 * <p>
 * The protocol layer normally gets fed from the transport layer, but is independent from it (to be easily testable and
 * keeping transports pluggable).
 * </p>
 * <p>
 * To execute actual command functionality, the protocol layer looks up (using {@link org.apache.vysper.xmpp.protocol.StanzaHandlerLookup})
 * and hands over control to the appropriate {@link org.apache.vysper.xmpp.protocol.StanzaHandler}.
 * It acts as a state machine for a {@link org.apache.vysper.xmpp.server.SessionContext}.
 * </p>
 * <p>
 * Essentially, the protocol layer ensures that every XMPP session is in a well-defined state, the fundamental rules of stanza processing are
 * followed, that the appropriate handlers are called and session, transport and connection are working well together.
 * It knows what to do when a session gets closed (unexpectedly or expectedly) and handles stream level errors.
 * </p>
 * @see org.apache.vysper.xmpp.protocol.ProtocolWorker
 * @see org.apache.vysper.xmpp.stanza.Stanza
 * @see org.apache.vysper.xmpp.protocol.StanzaHandlerLookup
 * @see org.apache.vysper.xmpp.protocol.StanzaHandler
 * @see org.apache.vysper.xmpp.protocol.StateAwareProtocolWorker
 */
package org.apache.vysper.xmpp.protocol;
