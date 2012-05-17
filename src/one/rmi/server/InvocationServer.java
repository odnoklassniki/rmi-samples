package one.rmi.server;

import one.rmi.client.ConnectionString;
import one.rmi.net.Selector;
import one.rmi.net.Socket;
import one.rmi.net.SocketHandle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class InvocationServer implements InvocationServerMBean, Thread.UncaughtExceptionHandler {
    private static final Log log = LogFactory.getLog(InvocationServer.class);

    private volatile boolean running;

    protected AcceptorThread acceptor;
    protected SelectorThread[] selectors;
    protected WorkerPool workers;
    protected CleanupThread cleanup;
    protected int timeout;
    protected SessionType sessionType;
    protected RequestProcessor requestProcessor;
    protected AtomicInteger acceptedConnections;

    public InvocationServer(ConnectionString conn, RequestProcessor requestProcessor) throws IOException {
        InetAddress address = InetAddress.getByName(conn.getHost());
        int port = conn.getPort();
        int backlog = conn.getIntParam("backlog", 128);
        int buffers = conn.getIntParam("buffers", 0);
        int selectorCount = conn.getIntParam("selectors", Runtime.getRuntime().availableProcessors());
        int minWorkers = conn.getIntParam("minWorkers", 16);
        int maxWorkers = conn.getIntParam("maxWorkers", 1000);
        long queueTime = conn.getLongParam("queueTime", 0);
        int keepAlive = conn.getIntParam("keepalive", 0);

        this.acceptor = new AcceptorThread(this, address, port, backlog, buffers);
        this.selectors = new SelectorThread[selectorCount];
        this.workers = new WorkerPool(this, minWorkers, maxWorkers, queueTime);
        this.timeout = conn.getIntParam("timeout", 0);
        this.sessionType = SessionType.valueOf(conn.getStringParam("serialization", "jboss"));
        this.requestProcessor = requestProcessor;
        this.acceptedConnections = new AtomicInteger();

        if (keepAlive > 0) {
            this.cleanup = new CleanupThread(this, keepAlive);
        }

        if (conn.getBooleanParam("jmx", false)) {
            registerMBean();
        }
    }

    public boolean reconfigure(ConnectionString conn) {
        if (acceptor.address.getHostName().equals(conn.getHost()) && acceptor.port == conn.getPort()) {
            workers.setCorePoolSize(conn.getIntParam("minWorkers", 16));
            workers.setMaximumPoolSize(conn.getIntParam("maxWorkers", 1000));
            workers.setQueueTime(conn.getLongParam("queueTime", 0));
            timeout = conn.getIntParam("timeout", 0);
            sessionType = SessionType.valueOf(conn.getStringParam("serialization", "jboss"));
            return true;
        }
        return false;
    }

    public void start() {
        running = true;
        for (int i = 0; i < selectors.length; i++) {
            selectors[i] = new SelectorThread(this, i);
            selectors[i].start();
        }
        acceptor.start();
        if (cleanup != null) {
            cleanup.start();
        }
    }

    public void stop() {
        running = false;
        if (cleanup != null) {
            cleanup.shutdown();
            cleanup = null;
        }
        if (acceptor != null) {
            acceptor.shutdown();
            acceptor = null;
        }
        if (selectors != null) {
            for (SelectorThread selector : selectors) {
                selector.shutdown();
            }
            selectors = null;
        }
        if (workers != null) {
            workers.shutdownNow();
            workers = null;
        }
    }

    public final boolean isRunning() {
        return running;
    }

    @Override
    public int getPoolSize() {
        return workers.getPoolSize();
    }

    @Override
    public int getActiveCount() {
        return workers.getActiveCount();
    }

    @Override
    public int getConnections() {
        int result = 0;
        for (SelectorThread selectorThread : selectors) {
            result += selectorThread.selector.size();
        }
        return result;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.fatal("Fatal error in " + t, e);
    }

    final void addConnection(Socket socket) throws IOException {
        acceptedConnections.incrementAndGet();
        socket.setTimeout(timeout);
        SocketHandle handle = getSmallestSelector().register(socket);
        handle.attach(sessionType.newInstance(this, requestProcessor, handle));
        handle.activate();
    }
    
    final void processSession(Session session) {
        workers.execute(session);
    }

    private Selector getSmallestSelector() {
        Selector result = null;
        int minSize = Integer.MAX_VALUE;
        for (SelectorThread selectorThread : selectors) {
            int size = selectorThread.selector.size();
            if (size < minSize) {
                minSize = size;
                result = selectorThread.selector;
            }
        }
        return result;
    }
    
    private void registerMBean() {
        try {
            ObjectName name = new ObjectName("one.rmi.server:type=InvocationServer");
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
        } catch (Exception e) {
            log.error("Cannot register MBean", e);
        }
    }
}
