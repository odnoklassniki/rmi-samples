package one.rmi.server;

import one.rmi.net.SocketHandle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CleanupThread extends Thread {
    private static final Log log = LogFactory.getLog(CleanupThread.class);

    final InvocationServer server;
    final int keepAlive;

    CleanupThread(InvocationServer server, int keepAlive) {
        super("Blos Cleanup");
        setUncaughtExceptionHandler(server);
        this.server = server;
        this.keepAlive = keepAlive;
    }

    void shutdown() {
        interrupt();
        try {
            join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public void run() {
        for (;;) {
            try {
                Thread.sleep(keepAlive / 2);
            } catch (InterruptedException e) {
                break;
            }

            SelectorThread[] selectors = server.selectors;
            if (!server.isRunning() || selectors == null) {
                break;
            }

            long cleanTime = System.currentTimeMillis();
            long expireTime = cleanTime - keepAlive;
            int expireCount = 0;
            for (SelectorThread selectorThread : selectors) {
                for (SocketHandle handle : selectorThread.selector) {
                    Session session = (Session) handle.attachment();
                    if (session != null) {
                        long lastAccessTime = session.lastAccessTime;
                        if (lastAccessTime > 0 && lastAccessTime < expireTime) {
                            session.close();
                            expireCount++;
                        }
                    }
                }
            }
            
            if (log.isInfoEnabled()) {
                log.info(expireCount + " sessions closed in " + (System.currentTimeMillis() - cleanTime) + " ms");
            }
        }
    }
}
