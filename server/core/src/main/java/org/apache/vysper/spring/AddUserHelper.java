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
package org.apache.vysper.spring;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;

/**
 * helper to inject user accounts on spring context creation
 */
public class AddUserHelper {

    private final Map<String, String> userPasswordMap = new HashMap<String, String>();

    public AddUserHelper(Map<String, String> userPasswordMap) {
        this.userPasswordMap.putAll(userPasswordMap);
    }

    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry)
            throws AccountCreationException, EntityFormatException {
        AccountManagement accountManagement = (AccountManagement) storageProviderRegistry
                .retrieve(AccountManagement.class);
        if (accountManagement == null)
            throw new IllegalStateException("no account manager accessible.");

        for (String user : userPasswordMap.keySet()) {
            if (!accountManagement.verifyAccountExists(EntityImpl.parse(user))) {
                String password = userPasswordMap.get(user);
                if (StringUtils.isEmpty(password)) {
                    password = RandomStringUtils.randomAlphanumeric(8);
                    System.out.println(user + " user will be added with random password: '" + password + "'");
                }
                accountManagement.addUser(user, password);
            }
        }
    }
}
