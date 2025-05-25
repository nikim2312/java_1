package org.example;


// Main.java
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        RejectedExecutionHandler rejectionHandler = new MyRejectionHandler();

        CustomExecutor executor = new MyThreadPool(
                3,      // corePoolSize
                10,     // maxPoolSize
                60,     // idleTimeout
                TimeUnit.SECONDS,
                2,      // minIdle
                50,     // queueCapacity
                rejectionHandler
        );

        for (int i = 0; i < 60; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("Task " + taskId + " started by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    System.err.println("Task " + taskId + " interrupted: " + e.getMessage());
                }
                System.out.println("Task " + taskId + " finished by " + Thread.currentThread().getName());
            });
        }

        executor.shutdown();
    }
}