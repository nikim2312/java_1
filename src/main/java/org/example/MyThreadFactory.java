package org.example;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "TaskWorker-" + threadNumber.getAndIncrement());
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> System.err.println("Unhandled exception in thread: " + t.getName() + ", error: " + e.getMessage()));
        return thread;
    }
}