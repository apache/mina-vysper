package org.apache.vysper.storage.hbase;

/**
 */
public class HBaseStorageException extends Exception {
    public HBaseStorageException() {
        super();
    }

    public HBaseStorageException(String s) {
        super(s);
    }

    public HBaseStorageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public HBaseStorageException(Throwable throwable) {
        super(throwable);
    }
}
