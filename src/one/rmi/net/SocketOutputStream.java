package one.rmi.net;

import java.io.IOException;
import java.io.OutputStream;

public class SocketOutputStream extends OutputStream {
    protected final Socket socket;

    public SocketOutputStream(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] tmp = new byte[] { (byte) b };
        socket.write(tmp, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        socket.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        socket.write(b, off, len);
    }
}
