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
package org.apache.vysper.xmpp.authentication;

import java.util.HashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * very simple in-memory {@link org.apache.vysper.xmpp.authentication.UserAuthentication} service
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class SimpleUserAuthentication implements UserAuthentication, AccountManagement {

    private final Map<Entity, String> userPasswordMap = new HashMap<Entity, String>();

    public SimpleUserAuthentication() {
        ; // empty
    }

    public SimpleUserAuthentication(Map<Entity, String> userPasswordMap) {
        this.userPasswordMap.putAll(userPasswordMap);
    }

    public void addUser(Entity username, String password) {
        userPasswordMap.put(username, password);
    }

    public void changePassword(Entity username, String password) throws AccountCreationException {
        if (!userPasswordMap.containsKey(username)) {
            throw new AccountCreationException("could not change password for unknown user " + username);
        }
        userPasswordMap.put(username, password);
    }

    public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
        return verify(jid.getBareJID(), passwordCleartext);
    }

    public boolean verifyAccountExists(Entity jid) {
        return userPasswordMap.get(jid.getBareJID()) != null;
    }

    private boolean verify(Entity username, String passwordCleartext) {
        return passwordCleartext.equals(userPasswordMap.get(username));
    }
}
