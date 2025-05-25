package org.example;


import java.util.concurrent.RejectedExecutionException;

public class RejectedExecutionHandlerImpl {

    public void rejectedExecution(Runnable r, CustomExecutor executor) {
        System.err.println("[RejectionHandler] Task " + r + " has been turned away due to pool saturation.");
        throw new RejectedExecutionException("Task " + r + " bounced from " + executor);
    }
}
