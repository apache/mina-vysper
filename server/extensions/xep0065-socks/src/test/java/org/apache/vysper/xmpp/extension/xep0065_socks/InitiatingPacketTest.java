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

import java.net.UnknownHostException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.xmpp.extension.xep0065_socks.Socks5AcceptorHandler.InitiatingPacket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InitiatingPacketTest extends Mockito {

    @Before
    public void before() {
    }
    
    @Test
    public void validText() throws UnknownHostException {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 0, 3, 9, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74, 5, 6});
        InitiatingPacket p = new InitiatingPacket(b);
        
        Assert.assertEquals("localhost", p.getAddress());
        Assert.assertEquals(1286, p.getPort());
        
        IoBuffer expected = IoBuffer.wrap(new byte[]{5, 0, 0, 3, 9, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74, 5, 6});
        Assert.assertEquals(expected, p.createResponse());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void invalidVersion() {
        IoBuffer b = IoBuffer.wrap(new byte[]{4, 1, 0, 1, 1, 2, 3, 4, 5, 6});
        new InitiatingPacket(b);
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidCmdCode() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 2, 0, 1, 1, 2, 3, 4, 5, 6});
        new InitiatingPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void invalidReserved() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 1, 1, 1, 2, 3, 4, 5, 6});
        new InitiatingPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void missingPort() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 0, 1, 1, 2, 3, 4});
        new InitiatingPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void missingAddress() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 0, 1});
        new InitiatingPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void tooLong() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 0, 1, 1, 2, 3, 4, 5, 6, 7});
        new InitiatingPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void empty() {
        IoBuffer b = IoBuffer.wrap(new byte[]{});
        new InitiatingPacket(b);
    }
}
