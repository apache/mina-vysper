package org.apache.vysper.xmpp.delivery.inbound;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes tasks syncronously, useful for test purposes
 *
 */
public class TestExecutorService implements ExecutorService {

    public void execute(Runnable command) {
        command.run();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
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
        return null;
    }

    public <T> Future<T> submit(Callable<T> task) {
        try {
            return new TestFuture<T>(task.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public Future<?> submit(Runnable task) {
        task.run();
        return new TestFuture();
    }

    public <T> Future<T> submit(Runnable task, T result) {
        task.run();
        return new TestFuture<T>(result);
    }
    
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
}