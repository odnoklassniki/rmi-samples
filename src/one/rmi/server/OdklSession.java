package one.rmi.server;

import one.rmi.net.Socket;
import one.rmi.net.SocketHandle;
import one.rmi.serial.CalcSizeStream;
import one.rmi.serial.DeserializeStream;
import one.rmi.serial.SerializeStream;
import one.rmi.serial.SerializerNotFoundException;

import java.io.IOException;

class OdklSession extends Session<Object, Object> {
    private final Socket socket;

    OdklSession(InvocationServer server, RequestProcessor<Object, Object> requestProcessor, SocketHandle handle) throws IOException {
        super(server, requestProcessor, handle);
        this.socket = handle.socket();
    }

    private Object readRequest() throws IOException, ClassNotFoundException {
        byte[] buf = getInBuffer();
        socket.readFully(buf, 0, 4);
        if (buf[0] != 0) {
            throw new IOException("Invalid request header or request too large");
        }

        int requestSize = (buf[1] & 0xff) << 16 | (buf[2] & 0xff) << 8 | (buf[3] & 0xff);
        if (requestSize > BUFFER_SIZE) {
            buf = new byte[requestSize];
        }
        socket.readFully(buf, 0, requestSize);
        return new DeserializeStream(buf).readObject();
    }

    private void writeResponse(Object response) throws IOException {
        CalcSizeStream calcSizeStream = new CalcSizeStream();
        calcSizeStream.writeObject(response);
        int size = calcSizeStream.count();

        SerializeStream ss = new SerializeStream(getOutBuffer(size + 4));
        ss.writeInt(size);
        ss.writeObject(response);

        socket.write(ss.array(), 0, ss.count());
    }

    @Override
    public void run() {
        try {
            Object result;
            try {
                Object request = readRequest();
                result = requestProcessor.process(request);
            } catch (SerializerNotFoundException e) {
                result = e;
            }
            writeResponse(result);
            handle.renew();
        } catch (Throwable e) {
            reportException(e);
            close();
        }
    }
}
