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
package org.apache.vysper.storage.hbase.privatedata;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.vysper.storage.hbase.HBaseStorage;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0049_privatedata.PrivateDataPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.vysper.storage.hbase.HBaseStorage.*;
import static org.apache.vysper.storage.hbase.HBaseUtils.*;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HBasePrivateDataPersistenceManager implements PrivateDataPersistenceManager {

    final Logger logger = LoggerFactory.getLogger(HBasePrivateDataPersistenceManager.class);

    public static final String COLUMN_PREFIX_NAME = "xep_priv_data:";
    
    protected HBaseStorage hbaseStorage;

    public HBasePrivateDataPersistenceManager(HBaseStorage hbaseStorage) {
        this.hbaseStorage = hbaseStorage;
    }

    public boolean isAvailable() {
        HTableInterface table = null;
        try {
            table = hbaseStorage.getTable(TABLE_NAME_USER);
            return table != null;
        } finally {
            hbaseStorage.putTable(table);
        }
    }

    public String getPrivateData(Entity entity, String key) {
        final Result entityRow = hbaseStorage.getEntityRow(entity, COLUMN_FAMILY_NAME_XEP);

        String column = COLUMN_PREFIX_NAME + key;
        String value = toStr(entityRow.getValue(COLUMN_FAMILY_NAME_XEP_BYTES, asBytes(column)));
        
        return value;
    }

    public boolean setPrivateData(Entity entity, String key, String xml) {

        if (key == null || StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("private data key must not be blank");
        }
        String column = COLUMN_PREFIX_NAME + key;
        
        final Put put = new Put(entityAsBytes(entity.getBareJID()));
        put.add(COLUMN_FAMILY_NAME_XEP_BYTES, asBytes(column), asBytes(xml));

        HTableInterface table = null;
        try {
            table = hbaseStorage.getTable(TABLE_NAME_USER);
            table.put(put);
            logger.debug("stored private data for {} with key {}", entity, key);
            return true;
        } catch (IOException e) {
            logger.warn("failed to save private data for {} with key {}", entity, key);
            return false;
        } finally {
            hbaseStorage.putTable(table);
        }
    }

}
