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
package org.apache.vysper.storage.jcr.user;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.vysper.storage.jcr.JcrStorage;
import org.apache.vysper.storage.jcr.JcrStorageException;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.authentication.UserAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JcrUserManagement implements UserAuthentication, AccountManagement {

    final Logger logger = LoggerFactory.getLogger(JcrUserManagement.class);

    protected JcrStorage jcrStorage;

    private static final String CREDENTIALS_NAMESPACE = "vysper_internal_credentials";

    public JcrUserManagement(JcrStorage jcrStorage) {
        this.jcrStorage = jcrStorage;
    }

    public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
        if (passwordCleartext == null)
            return false;
        try {
            final Node credentialsNode = jcrStorage.getEntityNode(jid, CREDENTIALS_NAMESPACE, false);
            if (credentialsNode == null)
                return false;
            final Property property = credentialsNode.getProperty("password");
            if (property == null)
                return false;
            final String password = property.getValue().getString();
            return passwordCleartext.equals(password);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyCredentials(String username, String passwordCleartext, Object credentials) {
        try {
            return verifyCredentials(EntityImpl.parse(username), passwordCleartext, credentials);
        } catch (EntityFormatException e) {
            return false;
        }
    }

    public boolean verifyAccountExists(Entity jid) {
        try {
            return jcrStorage.getEntityNode(jid, CREDENTIALS_NAMESPACE, false) != null;
        } catch (JcrStorageException e) {
            return false;
        }
    }

    public void addUser(Entity username, String password) throws AccountCreationException {
        // if already existent, don't create, throw error
        try {
            if (jcrStorage.getEntityNode(username, CREDENTIALS_NAMESPACE, false) != null) {
                throw new AccountCreationException("account already exists: " + username.getFullQualifiedName());
            }
        } catch (JcrStorageException e) {
            throw new AccountCreationException("account exists check failed for " + username.getFullQualifiedName(), e);
        }
        // now, finally, create
        try {
            final Node credentialsNode = jcrStorage.getEntityNode(username, CREDENTIALS_NAMESPACE, true);
            credentialsNode.setProperty("password", password);
            credentialsNode.save();
            logger.info("JCR node created: " + credentialsNode);
        } catch (Exception e) {
            // TODO remove account?
            throw new AccountCreationException("failed to create the account set credentials", e);
        }

    }

    public void changePassword(Entity username, String password) throws AccountCreationException {
        try {
            final Node credentialsNode = jcrStorage.getEntityNode(username, CREDENTIALS_NAMESPACE, false);
            credentialsNode.setProperty("password", password);
            credentialsNode.save();
            logger.info("JCR password changed: " + credentialsNode);
        } catch (Exception e) {
            // TODO remove account?
            throw new AccountCreationException("failed to create the account set credentials", e);
        }
    }
}
