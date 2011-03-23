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

import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5PairTest extends Mockito {

    private String hash = "foo";
    private IoSession target = mock(IoSession.class);
    private IoSession requester = mock(IoSession.class);
    
    private Socks5ConnectionsRegistry connectionsRegistry = mock(Socks5ConnectionsRegistry.class);
    
    @Test
    public void constructor() {
        Socks5Pair pair = new Socks5Pair(target, connectionsRegistry, hash);

        Assert.assertEquals(target, pair.getTarget());
        Assert.assertNull(pair.getRequester());
        Assert.assertFalse(pair.isActivated());
    }

    @Test
    public void requester() {
        Socks5Pair pair = new Socks5Pair(target, connectionsRegistry, hash);
        pair.setRequester(requester);
        
        Assert.assertEquals(requester, pair.getRequester());
        Assert.assertFalse(pair.isActivated());
    }
    
    @Test
    public void getOther() {
        Socks5Pair pair = new Socks5Pair(target, connectionsRegistry, hash);
        pair.setRequester(requester);
        
        Assert.assertEquals(requester, pair.getOther(target));
        Assert.assertEquals(target, pair.getOther(requester));
    }
    
    @Test
    public void activation() {
        Socks5Pair pair = new Socks5Pair(target, connectionsRegistry, hash);
        pair.setRequester(requester);
        
        Assert.assertFalse(pair.isActivated());
        
        pair.activate();
        
        Assert.assertTrue(pair.isActivated());
    }
    
    @Test
    public void close() {
        Socks5Pair pair = new Socks5Pair(target, connectionsRegistry, hash);
        pair.setRequester(requester);
        
        pair.close();

        verify(target).close(false);
        verify(requester).close(false);
    }

}
