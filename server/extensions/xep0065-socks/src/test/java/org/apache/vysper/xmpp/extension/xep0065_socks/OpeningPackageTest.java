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

import java.util.EnumSet;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.xmpp.extension.xep0065_socks.Socks5AcceptorHandler.OpeningPacket;
import org.apache.vysper.xmpp.extension.xep0065_socks.Socks5AcceptorHandler.Socks5AuthType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class OpeningPackageTest extends Mockito {

    @Before
    public void before() {
    }
    
    @Test
    public void valid() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 2, 2, 0});
        OpeningPacket p = new OpeningPacket(b);
        
        Assert.assertEquals(EnumSet.of(Socks5AuthType.USERNAME_PASSWORD, Socks5AuthType.NO_AUTH), p.getAuthTypes());
        
        IoBuffer expected = IoBuffer.wrap(new byte[]{5, 0});
        Assert.assertEquals(expected, p.createResponse());
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidVersion() {
        IoBuffer b = IoBuffer.wrap(new byte[]{4, 2, 2, 0});
        new OpeningPacket(b);
    }

    @Test(expected=IllegalArgumentException.class)
    public void tooShort() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 2, 2});
        new OpeningPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void zeroAuth() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 0});
        new OpeningPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void tooLong() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5, 1, 0, 2});
        new OpeningPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void empty() {
        IoBuffer b = IoBuffer.wrap(new byte[]{});
        new OpeningPacket(b);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void missingAuth() {
        IoBuffer b = IoBuffer.wrap(new byte[]{5});
        new OpeningPacket(b);
    }
}
