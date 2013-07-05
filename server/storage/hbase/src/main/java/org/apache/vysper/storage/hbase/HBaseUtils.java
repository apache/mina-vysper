package org.apache.vysper.storage.hbase;

import org.apache.vysper.xmpp.addressing.Entity;

import java.io.UnsupportedEncodingException;

/**
 */
public class HBaseUtils {

    public static byte[] asBytes(String str) {
        if (str == null) return null;
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  // won't happen! UTF-8 is supported
            return null;
        }
    }

    public static byte[] entityAsBytes(Entity entity) {
        if (entity == null) return null;
        return asBytes(entity.getFullQualifiedName());
    }

    public static String toStr(byte[] bytes) {
        if (bytes == null) return null;
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null; // will not happen for UTF-8
        }
    }
}
