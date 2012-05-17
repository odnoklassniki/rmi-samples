package one.rmi.client;

import one.rmi.net.Socket;

import java.io.IOException;
import java.net.InetAddress;

class SocketWorker {
    Socket socket;
    byte[] buf;
    
    SocketWorker() {
        // empty
    }

    SocketWorker(int bufferSize) {
        this.buf = new byte[bufferSize];
    }

    final boolean isConnected() {
        return socket != null;
    }

    void connect(InetAddress address, int port, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.setTimeout(timeout);
        socket.setNoDelay(true);
        socket.connect(address, port);
        this.socket = socket;
    }

    void close() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
