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
package org.apache.vysper.storage.hbase.user;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.vysper.storage.hbase.HBaseStorage;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.authentication.UserAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.apache.vysper.storage.hbase.HBaseStorage.COLUMN_FAMILY_NAME_BASIC;
import static org.apache.vysper.storage.hbase.HBaseUtils.entityAsBytes;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HBaseUserManagement implements UserAuthentication, AccountManagement {

    final Logger logger = LoggerFactory.getLogger(HBaseUserManagement.class);

    public static final byte[] PASSWORD_COLUMN = "pwd".getBytes();
    
    protected HBaseStorage hBaseStorage;

    /**
     * the salt before encrypting all passwords
     * change once before creating the first account
     */
    private String encryptionSalt = "saltetForVysper";

    /**
     * the number of hashing rounds for encrypting all passwords
     * change once before creating the first account
     */
    private int hashingRounds = 5;
    
    public HBaseUserManagement(HBaseStorage hBaseStorage) {
        this.hBaseStorage = hBaseStorage;
    }

    public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
        if (passwordCleartext == null)
            return false;
        try {
            final Result entityRow = hBaseStorage.getEntityRow(jid, COLUMN_FAMILY_NAME_BASIC);
            if (entityRow == null) return false;

            final byte[] encryptedGivenPassword = encryptPassword(passwordCleartext);
            final byte[] passwordSavedBytes = entityRow.getValue(COLUMN_FAMILY_NAME_BASIC.getBytes(), PASSWORD_COLUMN);
            return Arrays.equals(passwordSavedBytes, encryptedGivenPassword);
        } catch (Exception e) {
            return false;
        }
    }

    protected byte[] encryptPassword(String passwordCleartext) {
        if (passwordCleartext == null) passwordCleartext = "";
        try {
            passwordCleartext = passwordCleartext + encryptionSalt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            int rounds = Math.max(1, hashingRounds);
            byte[] pwdBytes = passwordCleartext.getBytes("UTF-8");
            for (int i = 0; i < rounds; i++) {
                pwdBytes = digest.digest(pwdBytes);
            }
            return pwdBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        final Result entityRow = hBaseStorage.getEntityRow(jid, COLUMN_FAMILY_NAME_BASIC);
        return !entityRow.isEmpty();
    }

    public void addUser(Entity username, String password) throws AccountCreationException {
        // if already existent, don't create, throw error
        if (verifyAccountExists(username)) {
            throw new AccountCreationException("account already exists: " + username.getFullQualifiedName());
        }

        // now, finally, create
        try {
            // row is created when first column for it is created.
            setPasswordInHBase(username, password);
            logger.info("account created in HBase for " + username);
        } catch (Exception e) {
            throw new AccountCreationException("failed to creating in HBase account " + username, e);
        }

    }

    private void setPasswordInHBase(Entity username, String password) throws IOException {
        final Put put = new Put(entityAsBytes(username));
        put.add(COLUMN_FAMILY_NAME_BASIC.getBytes(), PASSWORD_COLUMN, encryptPassword(password));
        HTableInterface table = null;
        try {
            table = hBaseStorage.getTable(HBaseStorage.TABLE_NAME_USER);
            table.put(put);
        } finally {
            hBaseStorage.putTable(table);
        }
    }

    public void changePassword(Entity username, String password) throws AccountCreationException {
        try {
            setPasswordInHBase(username, password);
            logger.info("password changed for " + username);
        } catch (Exception e) {
            throw new AccountCreationException("failed to change password for " + username, e);
        }
    }
}
