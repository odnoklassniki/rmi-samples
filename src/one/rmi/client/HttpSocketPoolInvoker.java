package one.rmi.client;

import one.rmi.io.HTTPUtils;
import one.rmi.net.Socket;
import one.rmi.io.UTFUtils;

import java.io.IOException;
import java.net.InetAddress;

public class HttpSocketPoolInvoker extends SocketPoolInvoker {
    private static final int BUFFER_SIZE = 4000;
    private static final byte[] CONTENT_LENGTH = UTFUtils.toBytes("Content-Length: ");

    public HttpSocketPoolInvoker(InetAddress address, int port, int clientMaxPoolSize, int timeout) {
        super(address, port, clientMaxPoolSize, timeout);
    }

    @Override
    protected SocketWorker createSocketWorker() {
        return new SocketWorker(BUFFER_SIZE);
    }

    @Override
    public Object invoke(Object request) throws Exception {
        return get((String) request);
    }

    public byte[] get(String request) throws Exception {
        SocketWorker worker = borrowSocketWorker();
        try {
            boolean isNew = !worker.isConnected();
            if (isNew) {
                worker.connect(address, port, timeout);
            }

            for (;;) {
                Socket socket = worker.socket;
                byte[] buf = worker.buf;
                int length = UTFUtils.write(request, buf, 0);

                try {
                    socket.write(buf, 0, length);
                    length = socket.read(buf, 0, BUFFER_SIZE);
                } catch (IOException e) {
                    if (isNew) throw e;
                    worker = reconnectSocketWorker(worker);
                    isNew = true;
                    continue;
                }

                int headerEnd = HTTPUtils.findHeaderEnd(buf, 0, length);
                if (headerEnd < 0) {
                    throw new IOException("Invalid response header");
                }

                int contentLength = findContentLength(buf, headerEnd);
                byte[] result = new byte[contentLength];
                int dataBytes = length - headerEnd;
                System.arraycopy(buf, headerEnd, result, 0, dataBytes);
                socket.readFully(result, dataBytes, contentLength - dataBytes);
                returnSocketWorker(worker);
                return result;
            }
        } catch (Exception e) {
            invalidateSocketWorker(worker);
            throw e;
        }
    }

    private static int findContentLength(byte[] buf, int length) {
        int p = UTFUtils.indexOf(CONTENT_LENGTH, buf, 0, length);
        if (p < 0) {
            throw new IllegalArgumentException("Content-Length field not found");
        }

        int result = 0;
        for (p += CONTENT_LENGTH.length; ; p++) {
            int digit = buf[p] - '0';
            if (digit >= 0 && digit <= 9) {
                result = result * 10 + digit;
            } else if (buf[p] == 13 || buf[p] == 10) {
                return result;
            } else {
                throw new NumberFormatException("Invalid Content-Length field");
            }
        }
    }
}
