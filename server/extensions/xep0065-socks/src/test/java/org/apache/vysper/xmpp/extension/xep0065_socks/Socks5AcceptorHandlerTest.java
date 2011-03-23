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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.vysper.xmpp.extension.xep0065_socks.Socks5AcceptorHandler.ProxyState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5AcceptorHandlerTest extends Mockito {

    private Socks5ConnectionsRegistry connectionsRegistry = mock(Socks5ConnectionsRegistry.class);
    private IoSession session = mock(IoSession.class);
    
    private Socks5AcceptorHandler handler = new Socks5AcceptorHandler(connectionsRegistry);
    
    @Before
    public void before() {
    }
    
    @Test
    public void sessionOpened() throws Exception {
        handler.sessionOpened(session);
        
        verify(session).setAttribute(Socks5AcceptorHandler.STATE_KEY, ProxyState.OPENED);
    }

    @Test
    public void messageReceivedOpened() throws Exception {
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.OPENED);
        IoBuffer buffer = IoBuffer.wrap(new byte[] {5, 1, 0});
        
        handler.messageReceived(session, buffer);
        
        verify(session).setAttribute(Socks5AcceptorHandler.STATE_KEY, ProxyState.INITIATED);
        verify(session).write(IoBuffer.wrap(new byte[]{5, 0}));
    }
    
    @Test
    public void messageReceivedOpenedUsernamePasswordAuth() throws Exception {
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.OPENED);
        IoBuffer buffer = IoBuffer.wrap(new byte[] {5, 1, 2});
        
        handler.messageReceived(session, buffer);
        
        verify(session).write(IoBuffer.wrap(new byte[]{5, 0x55}));
        verify(session).close(false);
    }
    
    @Test
    public void messageReceivedOpenedInvalid() throws Exception {
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.OPENED);
        IoBuffer buffer = IoBuffer.wrap(new byte[] {1,2,3,4});
        
        handler.messageReceived(session, buffer);
        
        verify(session).close(false);
    }

    @Test
    public void messageReceivedInitiated() throws Exception {
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.INITIATED);
        IoBuffer buffer = IoBuffer.wrap(new byte[]{5, 1, 0, 3, 9, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74, 5, 6});
        
        handler.messageReceived(session, buffer);
        
        verify(session).setAttribute(Socks5AcceptorHandler.STATE_KEY, ProxyState.CONNECTED);
        
        IoBuffer expected = IoBuffer.wrap(new byte[]{5, 0, 0, 3, 9, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74, 5, 6});
        verify(session).write(expected);
    }
    
    @Test
    public void messageReceivedInitiatedInvalid() throws Exception {
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.INITIATED);
        IoBuffer buffer = IoBuffer.wrap(new byte[] {1,2,3,4});
        
        handler.messageReceived(session, buffer);
        
        verify(session).close(false);
    }
    
    @Test
    public void messageReceivedActivated() throws Exception {
        IoSession other = mock(IoSession.class);
        
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.CONNECTED);
        Socks5Pair pair = new Socks5Pair(session, connectionsRegistry, "foo");
        pair.setRequester(other);
        pair.activate();
        when(session.getAttribute(Socks5AcceptorHandler.PAIR_KEY)).thenReturn(pair);
        
        IoBuffer buffer = IoBuffer.wrap(new byte[]{0x6C, 0x6F, 0x63});
        
        handler.messageReceived(session, buffer);

        verify(other).write(buffer);
    }

    @Test
    public void messageReceivedNotActivated() throws Exception {
        IoSession other = mock(IoSession.class);
        
        when(session.getAttribute(Socks5AcceptorHandler.STATE_KEY)).thenReturn(ProxyState.CONNECTED);
        Socks5Pair pair = new Socks5Pair(session, connectionsRegistry, "foo");
        pair.setRequester(other);
        when(session.getAttribute(Socks5AcceptorHandler.PAIR_KEY)).thenReturn(pair);
        
        IoBuffer buffer = IoBuffer.wrap(new byte[]{0x6C, 0x6F, 0x63});
        
        handler.messageReceived(session, buffer);
        
        verify(session).close(false);
        verify(other).close(false);
    }
    

    @Test
    public void sessionIdle() throws Exception {
        IoSession other = mock(IoSession.class);
        
        String hash = "foo";
        Socks5Pair pair = new Socks5Pair(session, connectionsRegistry, hash);
        pair.setRequester(other);
        when(session.getAttribute(Socks5AcceptorHandler.PAIR_KEY)).thenReturn(pair);
        
        handler.sessionIdle(session, IdleStatus.READER_IDLE);
        
        verify(session).close(false);
        verify(other).close(false);
    }
    
    @Test
    public void exceptionCaughtPairEstablished() throws Exception {
        IoSession other = mock(IoSession.class);
        
        Socks5Pair pair = new Socks5Pair(session, connectionsRegistry, "foo");
        pair.setRequester(other);
        when(session.getAttribute(Socks5AcceptorHandler.PAIR_KEY)).thenReturn(pair);
        
        handler.exceptionCaught(session, new Exception());
        
        verify(session).close(false);
        verify(other).close(false);
    }
    
    @Test
    public void exceptionCaughtPairNotEstablished() throws Exception {
        handler.exceptionCaught(session, new Exception());
        
        verify(session).close(false);
    }

    @Test
    public void sessionClosed() throws Exception {
        IoSession other = mock(IoSession.class);
        
        Socks5Pair pair = new Socks5Pair(session, connectionsRegistry, "foo");
        pair.setRequester(other);
        when(session.getAttribute(Socks5AcceptorHandler.PAIR_KEY)).thenReturn(pair);
        
        handler.sessionClosed(session);
        
        verify(session).close(false);
        verify(other).close(false);
    }
    

}
