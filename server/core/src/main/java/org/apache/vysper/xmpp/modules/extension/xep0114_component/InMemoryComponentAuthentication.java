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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authorization.AccountCreationException;

/**
 * very simple in-memory {@link org.apache.vysper.xmpp.authorization.UserAuthorization} service
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InMemoryComponentAuthentication implements ComponentAuthentication, ComponentAuthenticationManagement {

    private final Map<Entity, String> userPasswordMap = new HashMap<Entity, String>();

    public void addComponent(Entity component, String secret) throws AccountCreationException {
        userPasswordMap.put(component, secret);
    }

    public void changeSecret(Entity component, String secret) throws AccountCreationException {
        userPasswordMap.put(component, secret);
    }

    public boolean exists(Entity component) {
        return userPasswordMap.containsKey(component);
    }

    public boolean verifyCredentials(Entity component, String handshake, String streamId) {
        String secret = userPasswordMap.get(component);

        if(secret != null) {
            String expected = DigestUtils.shaHex(streamId + secret).toLowerCase();
            
            return expected.equals(handshake);
        } else {
            return false;
        }
    }
}
