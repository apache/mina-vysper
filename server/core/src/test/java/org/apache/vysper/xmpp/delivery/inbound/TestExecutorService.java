package org.apache.vysper.xmpp.delivery.inbound;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes tasks synchronously, useful for test purposes
 *
 */
public class TestExecutorService extends AbstractExecutorService {

    
    public static class TestFuture<V> implements Future<V> {

        private V value;

        public TestFuture() {
        }

        public TestFuture(V value) {
            this.value = value;
        }

        public boolean cancel(boolean arg0) {
            return false;
        }

        public V get() throws InterruptedException, ExecutionException {
            return value;
        }

        public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
            return value;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    public boolean isShutdown() {
        return false;
    }

    public boolean isTerminated() {
        return false;
    }

    public void shutdown() {
    }

    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    public void execute(Runnable runnable) {
        runnable.run();
        
    }
}