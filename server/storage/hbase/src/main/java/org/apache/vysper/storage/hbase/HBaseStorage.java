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
package org.apache.vysper.storage.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.vysper.xmpp.addressing.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.apache.vysper.storage.hbase.HBaseUtils.*;

/**
 * back-end adaptor for HBase
 * 
 * prepare HBase by creating table vysper_user:
 * create 'vysper_user', {NAME => 'bsc', VERSIONS => 1}, {NAME => 'cct', VERSIONS => 1}, {NAME => 'rst', VERSIONS => 1}, {NAME => 'xep', VERSIONS => 5}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HBaseStorage {

    final Logger LOG = LoggerFactory.getLogger(HBaseStorage.class);

    public static final String TABLE_NAME_USER = "vysper_user";
    public static final String COLUMN_FAMILY_NAME_BASIC = "bsc";
    public static final String COLUMN_FAMILY_NAME_CONTACT = "cct";
    public static final byte[] COLUMN_FAMILY_NAME_CONTACT_BYTES = COLUMN_FAMILY_NAME_CONTACT.getBytes();
    public static final String COLUMN_FAMILY_NAME_ROSTER = "rst";
    public static final byte[] COLUMN_FAMILY_NAME_ROSTER_BYTES = COLUMN_FAMILY_NAME_ROSTER.getBytes();
    public static final String COLUMN_FAMILY_NAME_XEP = "xep";
    public static final byte[] COLUMN_FAMILY_NAME_XEP_BYTES = COLUMN_FAMILY_NAME_XEP.getBytes();
    
    protected static HBaseStorage hbaseStorageSingleton;

    protected HBaseStorage() {
        super();
        // protected
    }

    public static HBaseStorage getInstance() throws HBaseStorageException {
        if (hbaseStorageSingleton != null) return hbaseStorageSingleton;
        synchronized (HBaseStorage.class) {
            if (hbaseStorageSingleton == null) hbaseStorageSingleton = new HBaseStorage();
            hbaseStorageSingleton.init();
            return hbaseStorageSingleton;
        }
    }

    protected Configuration hbaseConfiguration = null;
    protected HBaseAdmin hbaseAdmin;
    protected HTablePool tablePool;

    public void init() throws HBaseStorageException {
        try {
            hbaseConfiguration = HBaseConfiguration.create();
        } catch (Exception e) {
            throw new HBaseStorageException("failed to load HBase configuration from file hbase-site.xml");
        }
        final int size = hbaseConfiguration.size();
        if (size == 0) throw new HBaseStorageException("HBase configuration is empty");

        try {
            connectHBase();
        } catch (HBaseStorageException e) {
            LOG.error("connection to HBase failed", e);
            throw e;
        }
    }

    protected void connectHBase() throws HBaseStorageException {
        try {
            LOG.info("connecting to HBase...");
            hbaseAdmin = new HBaseAdmin(hbaseConfiguration);
            tablePool = new HTablePool(hbaseConfiguration, Integer.MAX_VALUE);
            LOG.info("HBase connected.");
        } catch (MasterNotRunningException e) {
            throw new HBaseStorageException("failed connecting to HBase Master Server", e);
        } catch (ZooKeeperConnectionException e) {
            throw new HBaseStorageException("failed connecting to HBase Zookeeper Cluster", e);
        }
    }

    public HTableInterface getTable(String tableName) {
        return tablePool.getTable(tableName);
    }

    public Result getEntityRow(Entity entity, String... columnFamilyNames) {
        if (columnFamilyNames == null || columnFamilyNames.length == 0) {
            columnFamilyNames = new String[]{COLUMN_FAMILY_NAME_CONTACT};
        }

        HTableInterface userTable = null;
        try {
            userTable = getTable(TABLE_NAME_USER);
            final Get get = new Get(entityAsBytes(entity.getBareJID()));
            for (String columnFamilyName : columnFamilyNames) {
                get.addFamily(asBytes(columnFamilyName));
            }
            final Result result = userTable.get(get);
            return result;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        } finally {
            putTable(userTable);
        }
    }

    public void putTable(HTableInterface userTable) {
        if (userTable == null) return;
        try {
            userTable.close();
        } catch (IOException e) {
            String tableName = "unknown";
            try {
                tableName = new String(userTable.getTableName(), "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();  // encoding exceptions are killing me
            }
            LOG.warn("failed to return table " + tableName + " to pool");
        }
    }

}
