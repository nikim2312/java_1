package org.example;

// CustomThreadPool.java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class MyThreadPool implements CustomExecutor {

    private final int coreThreads;
    private final int maxThreads;
    private final long idleTimeout;
    private final TimeUnit timeUnit;
    private final int minIdle;
    private final int queueCapacity;
    private final RejectedExecutionHandler rejectionHandler; // Using the standard interface
    private final MyThreadFactory threadFactory;
    private final BlockingQueue<Runnable> taskQueue;

    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger idleCount = new AtomicInteger(0);
    private volatile boolean isStopped = false;
    private final List<Worker> workers = new ArrayList<>();
    private final Object workerLock = new Object();

    public MyThreadPool(int coreThreads, int maxThreads, long idleTimeout, TimeUnit timeUnit, int minIdle, int queueCapacity, RejectedExecutionHandler rejectionHandler) {
        if (coreThreads < 0 || maxThreads <= 0 || maxThreads < coreThreads || idleTimeout < 0 || minIdle < 0 || queueCapacity < 0) {
            throw new IllegalArgumentException("Invalid thread pool configuration parameters.");
        }
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.idleTimeout = idleTimeout;
        this.timeUnit = timeUnit;
        this.minIdle = minIdle;
        this.queueCapacity = queueCapacity;
        this.rejectionHandler = rejectionHandler;
        this.threadFactory = new MyThreadFactory();
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);  // Changed to LinkedBlockingQueue

        initializeWorkers();
    }

    private void initializeWorkers() {
        synchronized (workerLock) {
            for (int i = 0; i < coreThreads; i++) {
                Worker worker = new Worker();
                workers.add(worker);
                Thread thread = threadFactory.newThread(worker);
                worker.setThread(thread);
                thread.start();
                activeCount.incrementAndGet();
                idleCount.incrementAndGet(); // Initial worker threads are considered idle
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException("Task cannot be null.");
        }

        if (isStopped) {
            rejectionHandler.rejectedExecution(command, new ThreadPoolExecutor(coreThreads, maxThreads, idleTimeout, timeUnit, taskQueue));
            return; // Directly delegate to rejection policy during shutdown
        }

        try {
            boolean offered = taskQueue.offer(command, 500, TimeUnit.MILLISECONDS); // Non-blocking offer with a timeout

            if (!offered){
                System.out.println("[Pool] Task offer timed out. Rejection handler invoked.");
                rejectionHandler.rejectedExecution(command, new ThreadPoolExecutor(coreThreads, maxThreads, idleTimeout, timeUnit, taskQueue));
                return;
            }

            if (activeCount.get() < maxThreads && taskQueue.size() > 0 && idleCount.get() ==0) { //Try to prevent pool saturation
                synchronized(workerLock){
                    if(activeCount.get() < maxThreads){
                        Worker worker = new Worker();
                        workers.add(worker);
                        Thread thread = threadFactory.newThread(worker);
                        worker.setThread(thread);
                        thread.start();
                        activeCount.incrementAndGet();
                        idleCount.incrementAndGet();
                        System.out.println("[Pool] Spawning extra thread.");
                    }
                }
            }


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rejectionHandler.rejectedExecution(command, new ThreadPoolExecutor(coreThreads, maxThreads, idleTimeout, timeUnit, taskQueue));
            return;
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isStopped = true;
        System.out.println("[Pool] Initiating graceful shutdown.  No new tasks will be accepted.");
        // No thread interruption, allow current tasks to complete
    }

    @Override
    public void shutdownNow() {
        isStopped = true;
        System.out.println("[Pool] Immediate shutdown requested. Interrupting active threads.");
        synchronized (workerLock) {
            for (Worker worker : workers) {
                worker.interrupt();  // Interrupt worker threads
            }
            taskQueue.clear(); // Clear any remaining tasks
        }
    }

    private class Worker implements Runnable {
        private Thread workerThread;

        public void setThread(Thread thread) {
            this.workerThread = thread;
        }

        public void interrupt() {
            if (workerThread != null) {
                workerThread.interrupt();
            }
        }

        @Override
        public void run() {
            try {
                Runnable task;
                while (!isStopped) {
                    try {
                        idleCount.decrementAndGet();
                        task = taskQueue.poll(idleTimeout, timeUnit); //Use blocking queue to retrieve tasks
                        if (task != null) {
                            try {
                                task.run();  // Execute the task
                            } catch (Throwable e) {
                                System.err.println("Exception during task execution: " + e.getMessage());
                            } finally {
                                idleCount.incrementAndGet();

                            }

                        } else {
                            // No task after timeout: consider termination
                            synchronized (workerLock) {
                                if (activeCount.get() > coreThreads) {

                                    activeCount.decrementAndGet();
                                    idleCount.decrementAndGet();
                                    workers.remove(this);
                                    System.out.println("[Worker] Worker timeout: Terminating idle thread.");
                                    return;  //Terminate the thread
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        Thread.interrupted(); //Clear interrupt flag
                    }
                }
            } finally {

                System.out.println("[Worker] Thread exiting worker loop.");
                idleCount.decrementAndGet();
                synchronized (workerLock) {
                    workers.remove(this);
                }

            }

        }
    }
}