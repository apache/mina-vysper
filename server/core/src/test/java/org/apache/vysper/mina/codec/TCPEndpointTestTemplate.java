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
package org.apache.vysper.mina.codec;

import java.io.IOException;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.TCPEndpoint;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


/**
 */
public abstract class TCPEndpointTestTemplate {

    private TCPEndpoint endpoint = createEndpoint();
    
    protected abstract TCPEndpoint createEndpoint();
    protected abstract int getDefaultPort();
    
    @Test
    public void getPort() throws IOException {
        Assert.assertEquals(getDefaultPort(), endpoint.getPort());
        endpoint.setPort(0);
        Assert.assertEquals(0, endpoint.getPort());
        
        endpoint.start();
        
        Assert.assertTrue(0 != endpoint.getPort());
    }
    
    @Test(expected=IllegalStateException.class)
    public void setPortAfterStarted() throws IOException {
        endpoint.setPort(0);
        endpoint.start();
        
        endpoint.setPort(12345);
    }
    
    @After
    public void tearDown() {
        endpoint.stop();
    }

}
