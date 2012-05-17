package one.rmi.server;

import one.rmi.net.SocketHandle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketException;
import java.net.SocketTimeoutException;

abstract class Session<Q, R> implements Runnable {
    private static final Log log = LogFactory.getLog(Session.class);

    static final int BUFFER_SIZE = 4000;

    final InvocationServer server;
    final RequestProcessor<Q, R> requestProcessor;
    final SocketHandle handle;

    protected volatile long lastAccessTime;
    protected boolean isClosed;

    Session(InvocationServer server, RequestProcessor<Q, R> requestProcessor, SocketHandle handle) {
        this.server = server;
        this.requestProcessor = requestProcessor;
        this.handle = handle;
    }

    final byte[] getInBuffer() {
        return ((WorkerThread) Thread.currentThread()).inBuffer;
    }

    final byte[] getInBuffer(int size) {
        return size <= BUFFER_SIZE ? ((WorkerThread) Thread.currentThread()).inBuffer : new byte[size];
    }

    final byte[] getOutBuffer() {
        return ((WorkerThread) Thread.currentThread()).outBuffer;
    }

    final byte[] getOutBuffer(int size) {
        return size <= BUFFER_SIZE ? ((WorkerThread) Thread.currentThread()).outBuffer : new byte[size];
    }

    protected synchronized void close() {
        if (!isClosed) {
            handle.cancel();
            handle.socket().close();
            isClosed = true;
        }
    }

    protected void reportException(Throwable e) {
        if (server.isRunning()) {
            if (e.getClass() == SocketException.class || e.getClass() == SocketTimeoutException.class) {
                if (log.isDebugEnabled()) {
                    log.debug("Client [" + clientIp() + "] has closed the connection", e);
                }
            } else if (log.isErrorEnabled()) {
                log.error("Error during remote invocation from [" + clientIp() + "]", e);
            }
        }
    }

    protected String clientIp() {
        byte[] address = new byte[4];
        if (handle.socket().getpeername(address) < 0) {
            return "<unconnected>";
        }
        return (address[0] & 0xff) + "." + (address[1] & 0xff) + "." + (address[2] & 0xff) + "." + (address[3] & 0xff);
    }
}
