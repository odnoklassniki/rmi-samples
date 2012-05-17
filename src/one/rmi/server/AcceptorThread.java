package one.rmi.server;

import one.rmi.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;

class AcceptorThread extends Thread {
    private static final Log log = LogFactory.getLog(AcceptorThread.class);
    
    final InvocationServer server;
    final InetAddress address;
    final int port;
    final Socket serverSocket;

    AcceptorThread(InvocationServer server, InetAddress address, int port, int backlog, int buffers) throws IOException {
        super("Blos Acceptor");
        setUncaughtExceptionHandler(server);
        this.server = server;
        this.address = address;
        this.port = port;
        this.serverSocket = new Socket();
        if (buffers != 0) {
            serverSocket.setBufferSize(buffers, buffers);
        }
        serverSocket.setReuseAddr(true);
        serverSocket.bind(address, port, backlog);
    }

    void shutdown() {
        serverSocket.close();
        try {
            join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    @Override
    public void run() {
        while (server.isRunning()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                server.addConnection(socket);
            } catch (Exception e) {
                if (server.isRunning()) {
                    log.error("Cannot accept incoming connection", e);
                }
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }
}
