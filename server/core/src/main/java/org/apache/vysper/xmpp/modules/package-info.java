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
 * Every module implements a small portion of the core XMPP spec or one of its extensions.
 * <p>
 * These implementations come in the shape of {@link org.apache.vysper.xmpp.protocol.StanzaHandler}s where a module
 * contains all handlers for one (or two) XMPP namespaces. In addition to handlers, a module covers all the
 * backend functionality needed for the implementation and publishes services (via {@link org.apache.vysper.xmpp.modules.ServerRuntimeContextService})
 * to be used by others.
 * </p>
 * <p>
 * Many XMPP extensions link into service discovery (XEP-0030). Those modules extend {@link org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule}.
 * </p>
 * <p>
 * The roster module ({@link org.apache.vysper.xmpp.modules.roster})is a simple example.
 * It installs the {@link org.apache.vysper.xmpp.modules.roster.RosterDictionary}, which registers
 * {@link org.apache.vysper.xmpp.modules.roster.handler.RosterIQHandler} for the "jabber:iq:roster" namespace. Additionally, it
 * publishes a {@link org.apache.vysper.xmpp.modules.roster.persistence.RosterManager} implementation, responsible for storing and retrieving
 * a user's roster. The RosterManager service, which can be accessed through {@link org.apache.vysper.xmpp.server.ServerRuntimeContext#getServerRuntimeContextService},
 * is also used by the {@link org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandler}.
 * </p>
 */
package org.apache.vysper.xmpp.modules;
