package one.rmi.net;

import java.io.IOException;
import java.io.InputStream;

public class SocketInputStream extends InputStream {
    protected final Socket socket;

    public SocketInputStream(Socket socket) {
        this.socket = socket;
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        socket.read(tmp, 0, 1);
        return tmp[0] & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return socket.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return socket.read(b, off, len);
    }
}
