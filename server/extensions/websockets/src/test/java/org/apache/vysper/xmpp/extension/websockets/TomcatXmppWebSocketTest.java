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
package org.apache.vysper.xmpp.extension.websockets;

import java.io.IOException;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.WsOutbound;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TomcatXmppWebSocketTest {

    private StanzaProcessor stanzaProcessor = Mockito.mock(StanzaProcessor.class);

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);

    private WsOutbound outbound = Mockito.mock(WsOutbound.class);

    @Test
    public void onMessage() throws IOException {
        TomcatXmppWebSocket context = new TomcatXmppWebSocket(serverRuntimeContext, stanzaProcessor);
        context.onTextMessage(CharBuffer.wrap("<test></test>"));

        Stanza expected = new StanzaBuilder("test").build();
        Mockito.verify(stanzaProcessor).processStanza(Mockito.eq(serverRuntimeContext),
                Mockito.any(InternalSessionContext.class), Mockito.eq(expected), Mockito.any(SessionStateHolder.class));
    }

    @Test
    public void write() throws IOException {
        TomcatXmppWebSocket context = new TomcatXmppWebSocket(serverRuntimeContext, stanzaProcessor);
        context.onOpen(outbound);

        context.write("<test></test>");

        Mockito.verify(outbound).writeTextMessage(CharBuffer.wrap("<test></test>"));
    }

}
