package one.rmi.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Socket {
    protected int fd;
    
    public Socket() throws IOException {
        this.fd = socket0();
    }

    public Socket(int fd) {
        this.fd = fd;
    }
    
    public final Socket accept() throws IOException {
        return new Socket(accept0());
    }

    public final void connect(String host, int port) throws IOException {
        connect(InetAddress.getByName(host), port);
    }
    
    public final void bind(String host, int port, int backlog) throws IOException {
        bind(InetAddress.getByName(host), port, backlog);
    }
    
    public final void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public final int read(byte[] data) throws IOException {
        return read(data, 0, data.length);
    }

    public final void readFully(byte[] data) throws IOException {
        readFully(data, 0, data.length);
    }

    public final InetSocketAddress getLocalAddress() {
        byte[] address = new byte[4];
        int port = getsockname(address);
        try {
            return port >= 0 ? new InetSocketAddress(InetAddress.getByAddress(address), port) : null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public final InetSocketAddress getRemoteAddress() {
        byte[] address = new byte[4];
        int port = getpeername(address);
        try {
            return port >= 0 ? new InetSocketAddress(InetAddress.getByAddress(address), port) : null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static native int socket0() throws IOException;
    private final native int accept0() throws IOException;

    public final native void close();
    public final native void connect(InetAddress address, int port) throws IOException;
    public final native void bind(InetAddress address, int port, int backlog) throws IOException;
    public final native int writeRaw(long buf, int count) throws IOException;
    public final native void write(byte[] data, int offset, int count) throws IOException;
    public final native int readRaw(long buf, int count) throws IOException;
    public final native int read(byte[] data, int offset, int count) throws IOException;
    public final native void readFully(byte[] data, int offset, int count) throws IOException;
    public final native int getsockname(byte[] address);
    public final native int getpeername(byte[] address);
    public final native void setTimeout(int timeout);
    public final native void setKeepAlive(boolean keepAlive);
    public final native void setNoDelay(boolean noDelay);
    public final native void setReuseAddr(boolean reuseAddr);
    public final native void setBufferSize(int recvBuf, int sendBuf);

    static {
        NativeLibrary.load();
    }
}
