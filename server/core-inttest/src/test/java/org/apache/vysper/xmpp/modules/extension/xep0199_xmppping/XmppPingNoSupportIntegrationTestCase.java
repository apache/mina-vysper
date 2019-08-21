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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.packet.IQ;

/**
 */
public class XmppPingNoSupportIntegrationTestCase extends AbstractIntegrationTestCase {

    @Override
    protected void addModules(XMPPServer server) {
        // ping module not added
    }

    public void testClientServerPing() throws Exception {
        PingPacket pingRequest = new PingPacket();
        pingRequest.setType(IQ.Type.get);
        pingRequest.setTo(SERVER_DOMAIN);
        pingRequest.setFrom(TEST_USERNAME1);

        IQ result = (IQ) sendSync(client, pingRequest);

        assertNotNull(result);
        assertEquals(IQ.Type.error, result.getType());
        assertEquals(SERVER_DOMAIN, result.getFrom().toString());
        assertEquals("service-unavailable", result.getError().getCondition().toString());
    }
}
