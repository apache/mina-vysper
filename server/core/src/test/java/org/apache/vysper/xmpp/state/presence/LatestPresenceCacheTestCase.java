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
package org.apache.vysper.xmpp.state.presence;

import static org.mockito.Mockito.mock;

import org.apache.vysper.xmpp.modules.core.TestUser;
import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandler;
import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandlerBaseTestCase;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * abstract test case which re-usable for all LatestPresenceCache
 * implementations
 */
abstract public class LatestPresenceCacheTestCase extends PresenceHandlerBaseTestCase {

    protected PresenceHandler handler = new PresenceHandler();

    abstract protected LatestPresenceCache getCache();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ((DefaultServerRuntimeContext) sessionContext.getServerRuntimeContext()).setPresenceCache(getCache());
    }

    public void testGet() {

        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        sessionStateHolder.setState(SessionState.AUTHENTICATED);

        // pres 1
        XMPPCoreStanza initialPresence1 = sendInitialPresence(sessionStateHolder, initiatingUser);
        PresenceStanza presenceStanza1 = getCache().get(initiatingUser.getEntityFQ());
        assertSame(initialPresence1, presenceStanza1);
        assertSame(initialPresence1, getCache().getForBareJID(initiatingUser.getEntity().getBareJID()));

        // pres 2, same user
        XMPPCoreStanza initialPresence2 = sendInitialPresence(sessionStateHolder, anotherInterestedUser);
        PresenceStanza presenceStanza2 = getCache().get(anotherInterestedUser.getEntityFQ());
        assertSame(initialPresence2, presenceStanza2);
        assertSame(initialPresence2, getCache().getForBareJID(anotherInterestedUser.getEntity().getBareJID()));

        // re-retrieve the pres 1, still there
        PresenceStanza presenceStanza1_again = getCache().get(initiatingUser.getEntityFQ());
        assertSame(initialPresence1, presenceStanza1_again);
        assertSame(initialPresence2, getCache().getForBareJID(initiatingUser.getEntity().getBareJID()));

        // replace pres 1
        XMPPCoreStanza initialPresence1_1 = sendInitialPresence(sessionStateHolder, initiatingUser);
        PresenceStanza presenceStanza1_1 = getCache().get(initiatingUser.getEntityFQ());
        assertSame(initialPresence1_1, presenceStanza1_1);
        assertSame(initialPresence1_1, getCache().getForBareJID(initiatingUser.getEntity().getBareJID()));

    }

    public XMPPCoreStanza sendInitialPresence(SessionStateHolder sessionStateHolder, TestUser user) {
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(
                StanzaBuilder.createPresenceStanza(user.getEntityFQ(), null, null, null, null, null).build());
        handler.execute(initialPresence, sessionContext.getServerRuntimeContext(), true, sessionContext,
                sessionStateHolder, mock(StanzaBroker.class));
        return initialPresence;
    }
}
