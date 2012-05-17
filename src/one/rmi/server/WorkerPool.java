package one.rmi.server;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class WorkerPool extends ThreadPoolExecutor implements ThreadFactory {
    private final InvocationServer server;
    private final AtomicInteger index;

    WorkerPool(InvocationServer server, int minThreads, int maxThreads, long queueTime) {
        super(minThreads, maxThreads, 60L, TimeUnit.SECONDS, new WaitingSynchronousQueue<Runnable>(queueTime));
        setThreadFactory(this);
        this.server = server;
        this.index = new AtomicInteger();
    }

    void setQueueTime(long queueTime) {
        ((WaitingSynchronousQueue) getQueue()).queueTime = queueTime;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new WorkerThread(r, "Blos Worker #" + index.incrementAndGet(), server);
    }

    private static final class WaitingSynchronousQueue<E> extends SynchronousQueue<E> {
        long queueTime;

        WaitingSynchronousQueue(long queueTime) {
            this.queueTime = queueTime;
        }

        @Override
        public boolean offer(E element) {
            try {
                return super.offer(element, queueTime, TimeUnit.MICROSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
