package org.apache.vysper.storage.hbase;

import java.nio.charset.StandardCharsets;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 */
public class HBaseUtils {

    public static byte[] asBytes(String str) {
        if (str == null) return null;
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] entityAsBytes(Entity entity) {
        if (entity == null) return null;
        return asBytes(entity.getFullQualifiedName());
    }

    public static String toStr(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
