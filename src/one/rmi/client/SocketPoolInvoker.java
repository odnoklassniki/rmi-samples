package one.rmi.client;

import one.log.scheduler.ILoggingSchedulerTask;
import one.log.scheduler.LoggingScheduler;
import one.log.util.LoggerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;

public abstract class SocketPoolInvoker {
    private static final Log statsLog = LogFactory.getLog("one.rmi.client.stats");

    private final LinkedList<SocketWorker> queue = new LinkedList<SocketWorker>();
    private int workers;
    private boolean isClosed;
    
    protected InetAddress address;
    protected int port;
    protected int clientMaxPoolSize;
    protected int timeout;

    protected SocketPoolInvoker(ConnectionString conn) throws Exception {
        this(InetAddress.getByName(conn.getHost()), conn.getPort(),
             conn.getIntParam("clientMaxPoolSize", 10),
             conn.getIntParam("timeout", 3000));

        initSocketPool(conn.getIntParam("clientMinPoolSize", 0));
    }
    
    protected SocketPoolInvoker(InetAddress address, int port, int clientMaxPoolSize, int timeout) {
        this.address = address;
        this.port = port;
        this.clientMaxPoolSize = clientMaxPoolSize;
        this.timeout = timeout;

        LoggingScheduler.registerScheduling(getStatsLogger());
    }

    public void close() {
        synchronized (queue) {
            invalidateAll();
            workers = 0;
            isClosed = true;
        }
    }
    
    public abstract Object invoke(Object request) throws Exception;

    // Get a pooled or a new unconnected SocketWorker
    protected final SocketWorker borrowSocketWorker() throws SocketPoolException, InterruptedException {
        synchronized (queue) {
            for (long breakTime = 0; ; ) {
                // First, try to get a pooled connection
                SocketWorker worker = queue.pollFirst();
                if (worker != null) {
                    return worker;
                }

                if (isClosed) {
                    throw new SocketPoolException("Socket pool is closed");
                }

                // If there is no free connection, but the pool is not yet full, create a new SocketWorker
                if (workers < clientMaxPoolSize) {
                    workers++;
                    return createSocketWorker();
                }

                // Wait up to timeout ms until there is a SocketWorker to borrow or an empty place in the pool
                long currentTime = System.currentTimeMillis();
                if (breakTime == 0) {
                    breakTime = currentTime + timeout;
                } else if (currentTime >= breakTime) {
                    throw new SocketPoolException("Borrow socket timed out");
                }
                queue.wait(breakTime - currentTime);
            }
        }
    }

    // Replace a stale SocketWorker with a fresh one, leaving the number of active workers unchanged
    protected final SocketWorker reconnectSocketWorker(SocketWorker oldWorker) throws IOException {
        SocketWorker newWorker = createSocketWorker();
        newWorker.connect(address, port, timeout);
        oldWorker.close();
        return newWorker;
    }
    
    // Return a SocketWorker to the pool for reusing in the future
    protected final void returnSocketWorker(SocketWorker worker) {
        synchronized (queue) {
            if (!isClosed) {
                if (queue.isEmpty() && workers == clientMaxPoolSize) {
                    queue.notify();
                }
                queue.addFirst(worker);
            } else {
                worker.close();
            }
        }
    }

    // Decrease the number of active workers and close the socket out of the lock
    protected final void invalidateSocketWorker(SocketWorker worker) {
        synchronized (queue) {
            if (!isClosed && workers-- == clientMaxPoolSize && queue.isEmpty()) {
                queue.notify();
            }
        }
        worker.close();
    }

    // Close all active connections
    public final void invalidateAll() {
        synchronized (queue) {
            for (SocketWorker worker : queue) {
                worker.close();
            }
            workers -= queue.size();
            queue.clear();
            queue.notifyAll();
        }
    }
    
    // Initialize the pool with the given number of established connections
    protected final void initSocketPool(int connections) {
        try {
            for (int i = 0; i < connections; i++) {
                SocketWorker worker = createSocketWorker();
                worker.connect(address, port, timeout);
                synchronized (queue) {
                    workers++;
                    queue.addLast(worker);
                }
            }
        } catch (IOException e) {
            // Leave the pool uninitialized
        }
    }

    protected SocketWorker createSocketWorker() {
        return new SocketWorker();
    }

    protected ILoggingSchedulerTask getStatsLogger() {
        return new ILoggingSchedulerTask() {
            @Override
            public int getPeriod() {
                return 5;
            }

            @Override
            public boolean isStop() {
                return isClosed;
            }

            @Override
            public void performLog() {
                if (!isClosed && statsLog.isTraceEnabled()) {
                    int poolSize, activeCount;
                    synchronized (queue) {
                        poolSize = workers;
                        activeCount = poolSize - queue.size();
                    }
                    statsLog.trace(LoggerUtil.unionMessages(address, poolSize, activeCount));
                }
            }
        };
    }

    public static SocketPoolInvoker create(String url) throws Exception {
        ConnectionString conn = new ConnectionString(url);
        if ("jboss".equals(conn.getStringParam("serializationType", "jboss"))) {
            return new JBossSocketPoolInvoker(conn);
        } else {
            return new OdklSocketPoolInvoker(conn);
        }
    }
}
