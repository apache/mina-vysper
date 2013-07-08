package org.apache.vysper.storage.hbase;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.vysper.xmpp.addressing.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.vysper.storage.hbase.HBaseStorage.COLUMN_FAMILY_NAME_XEP;
import static org.apache.vysper.storage.hbase.HBaseStorage.COLUMN_FAMILY_NAME_XEP_BYTES;
import static org.apache.vysper.storage.hbase.HBaseStorage.TABLE_NAME_USER;
import static org.apache.vysper.storage.hbase.HBaseUtils.asBytes;
import static org.apache.vysper.storage.hbase.HBaseUtils.entityAsBytes;
import static org.apache.vysper.storage.hbase.HBaseUtils.toStr;

/**
 */
public abstract class HBaseGenericXEPDataManager {
    final Logger logger = LoggerFactory.getLogger(HBaseGenericXEPDataManager.class);
    
    protected HBaseStorage hbaseStorage;

    public HBaseGenericXEPDataManager(HBaseStorage hbaseStorage) {
        this.hbaseStorage = hbaseStorage;
    }

    protected abstract String getNamespace();

    protected String getColumnForKey(String key) {
        return getNamespace() + "#"+ key;
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

    protected boolean setValue(Entity entity, String key, String xml) {
        if (key == null || StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key must not be blank, empty or null");
        }
        String column = getColumnForKey(key);

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

    protected String getValue(Entity entity, String key) {
        final Result entityRow = hbaseStorage.getEntityRow(entity, COLUMN_FAMILY_NAME_XEP);

        String column = getColumnForKey(key);
        return toStr(entityRow.getValue(COLUMN_FAMILY_NAME_XEP_BYTES, asBytes(column)));
    }
}
