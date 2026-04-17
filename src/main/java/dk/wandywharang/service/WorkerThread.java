package dk.wandywharang.service;

import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThread extends Thread {
    private static final AtomicInteger threadCount = new AtomicInteger(0);

    public WorkerThread(Runnable task) {
        super(task, "worker-" + threadCount.incrementAndGet());
    }
}
