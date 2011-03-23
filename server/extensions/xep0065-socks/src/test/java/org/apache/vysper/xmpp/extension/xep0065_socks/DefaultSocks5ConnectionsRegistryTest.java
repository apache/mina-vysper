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
public class DefaultSocks5ConnectionsRegistryTest extends Mockito {

    private String hash = "foo";
    private IoSession target = mock(IoSession.class);
    private IoSession requester = mock(IoSession.class);
    
    @Test
    public void register() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        connectionsRegistry.register(hash, target);
        
        Assert.assertEquals(target, connectionsRegistry.getTarget(hash));
        Assert.assertNull(connectionsRegistry.getRequester(hash));
    }

    @Test
    public void getTargetUnknownHash() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        Assert.assertNull(connectionsRegistry.getTarget(hash));
    }

    @Test
    public void getRequesterUnknownHash() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        Assert.assertNull(connectionsRegistry.getRequester(hash));
    }
    
    @Test
    public void registerRequester() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        connectionsRegistry.register(hash, target);
        connectionsRegistry.register(hash, requester);
        
        Assert.assertEquals(target, connectionsRegistry.getTarget(hash));
        Assert.assertEquals(requester, connectionsRegistry.getRequester(hash));
    }
    
    @Test
    public void activate() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        connectionsRegistry.register(hash, target);
        connectionsRegistry.register(hash, requester);
        
        Assert.assertFalse(connectionsRegistry.isActivated(hash));

        Assert.assertTrue(connectionsRegistry.activate(hash));

        Assert.assertTrue(connectionsRegistry.isActivated(hash));
    }
    
    @Test
    public void activateUnknownHash() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        Assert.assertFalse(connectionsRegistry.isActivated(hash));

        Assert.assertFalse(connectionsRegistry.activate(hash));

        Assert.assertFalse(connectionsRegistry.isActivated(hash));
    }

    @Test
    public void close() {
        DefaultSocks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
        
        connectionsRegistry.register(hash, target);
        connectionsRegistry.register(hash, requester);
        
        Assert.assertEquals(target, connectionsRegistry.getTarget(hash));
        
        connectionsRegistry.close(hash);
        
        Assert.assertNull(connectionsRegistry.getTarget(hash));
    }

}
