package org.example;

// CustomRejectionHandler.java
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class MyRejectionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) { // Standard interface method signature
        System.err.println("[CustomRejectionHandler] Task rejected: " + r.toString() + " from pool: " + executor.toString());
        // Implement your custom rejection logic here.  Options:

        // 1. Run the task in the current thread (CallerRunsPolicy)
        if (!executor.isShutdown()) {
            r.run(); // Be very cautious with this!
        } else {
            System.err.println("Pool is shut down, cannot run task.");
        }

        // 2. Discard the task silently (DiscardPolicy)
        // (Just do nothing)

        // 3. Throw an exception
        //throw new RejectedExecutionException("Task rejected by CustomThreadPool.");
    }
}