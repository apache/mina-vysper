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

package org.apache.vysper.xmpp.modules.extension.xep0114_component;

import junit.framework.TestCase;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.junit.Assert;

/**
 */
public class InMemoryComponentAuthenticationTestCase extends TestCase {

    private static final Entity COMPONENT = EntityImpl.parseUnchecked("comp.vysper.org");
    private static final String SECRET = "sekrit";
    private static final String STREAM_ID = "123456";

    private InMemoryComponentAuthentication componentAuthentication = new InMemoryComponentAuthentication();
    
    @Override
    public void setUp() throws AccountCreationException {
    }
    
    public void testVerifyCredentials() throws AccountCreationException {
        componentAuthentication.addComponent(COMPONENT, SECRET);
        String handshake = DigestUtils.shaHex(STREAM_ID + SECRET).toLowerCase();
        
        Assert.assertTrue(componentAuthentication.verifyCredentials(COMPONENT, handshake, STREAM_ID));
    }

    public void testVerifyUppercaseCredentials() throws AccountCreationException {
        componentAuthentication.addComponent(COMPONENT, SECRET);
        String handshake = DigestUtils.shaHex(STREAM_ID + SECRET).toUpperCase();
        
        Assert.assertFalse(componentAuthentication.verifyCredentials(COMPONENT, handshake, STREAM_ID));
    }

    public void testVerifyCredentialsMissingComponent() throws AccountCreationException {
        String handshake = DigestUtils.shaHex(STREAM_ID + SECRET).toUpperCase();
        
        Assert.assertFalse(componentAuthentication.verifyCredentials(COMPONENT, handshake, STREAM_ID));
    }

    public void testVerifyComponent() throws AccountCreationException {
        componentAuthentication.addComponent(COMPONENT, SECRET);
        Assert.assertTrue(componentAuthentication.exists(COMPONENT));
    }
    
}
