package one.rmi.server;

import one.rmi.net.Selector;
import one.rmi.net.SelectorFactory;
import one.rmi.net.SocketHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

class SelectorThread extends Thread {
    private static final Log log = LogFactory.getLog(SelectorThread.class);

    final InvocationServer server;
    final Selector selector;

    SelectorThread(InvocationServer server, int num) {
        super("Blos Selector #" + num);
        setUncaughtExceptionHandler(server);
        this.server = server;
        this.selector = SelectorFactory.create();
    }

    void shutdown() {
        selector.close();
        try {
            join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public void run() {
        while (server.isRunning()) {
            for (Iterator<SocketHandle> selectedHandles = selector.select(); selectedHandles.hasNext(); ) {
                SocketHandle handle = selectedHandles.next();
                Session session = (Session) handle.attachment();
                if (session != null) {
                    try {
                        server.processSession(session);
                    } catch (RejectedExecutionException e) {
                        if (server.isRunning()) {
                            log.error("Cannot process session: all worker threads are busy");
                        }
                        session.close();
                    }
                }
            }
        }
    }
}
