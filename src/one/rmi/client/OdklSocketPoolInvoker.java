package one.rmi.client;

import one.app.remote.comp.RemoteMethodCallRequest;
import one.app.remote.reflect.MethodId;
import one.rmi.serial.CalcSizeStream;
import one.rmi.serial.DeserializeStream;
import one.rmi.serial.Repository;
import one.rmi.serial.SerializeStream;
import one.rmi.serial.Serializer;
import one.rmi.serial.SerializerNotFoundException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;

public class OdklSocketPoolInvoker extends SocketPoolInvoker {
    private static final int BUFFER_SIZE = 1024;
    private static final MethodId requestSerializerMethod = new MethodId("requestSerializer", new Type[] { long.class });
    private static final MethodId provideSerializerMethod = new MethodId("provideSerializer", new Type[] { Serializer.class });

    public OdklSocketPoolInvoker(ConnectionString conn) throws Exception {
        super(conn);
    }

    @Override
    protected SocketWorker createSocketWorker() {
        return new SocketWorker(BUFFER_SIZE);
    }

    @Override
    public Object invoke(Object request) throws Exception {
        for (;;) {
            Object result = deserialize(getResponse(request));
            if (!(result instanceof SerializerNotFoundException)) {
                return result;
            }
            provideSerializer(((SerializerNotFoundException) result).getUid());
        }
    }

    private Object deserialize(byte[] response) throws Exception {
        for (;;) {
            try {
                return new DeserializeStream(response).readObject();
            } catch (SerializerNotFoundException e) {
                requestSerializer(e.getUid());
            }
        }
    }

    private byte[] getResponse(Object request) throws Exception {
        SocketWorker worker = sendRequest(request);
        try {
            byte[] buf = worker.buf;
            worker.socket.readFully(buf, 0, 4);
            if (buf[0] != 0) {
                throw new IOException("Invalid response header or response too large");
            }
            int responseSize = (buf[1] & 0xff) << 16 | (buf[2] & 0xff) << 8 | (buf[3] & 0xff);
            buf = new byte[responseSize];
            worker.socket.readFully(buf, 0, responseSize);
            returnSocketWorker(worker);
            return buf;
        } catch (Exception e) {
            invalidateSocketWorker(worker);
            throw e;
        }
    }

    private SocketWorker sendRequest(Object request) throws Exception {
        CalcSizeStream calcSizeStream = new CalcSizeStream();
        calcSizeStream.writeObject(request);
        int requestSize = calcSizeStream.count();
        
        SocketWorker worker = borrowSocketWorker();
        try {
            if (!worker.isConnected()) {
                worker.connect(address, port, timeout);
            }

            byte[] buf = requestSize <= BUFFER_SIZE - 4 ? worker.buf : new byte[requestSize + 4];
            SerializeStream ss = new SerializeStream(buf);
            ss.writeInt(requestSize);
            ss.writeObject(request);

            try {
                worker.socket.write(buf, 0, requestSize + 4);
            } catch (SocketException e) {
                worker = reconnectSocketWorker(worker);
                worker.socket.write(buf, 0, requestSize + 4);
            }
        } catch (Exception e) {
            invalidateSocketWorker(worker);
            throw e;
        }
        return worker;
    }
    
    private void provideSerializer(long uid) throws Exception {
        Serializer serializer = Repository.requestSerializer(uid);
        getResponse(new RemoteMethodCallRequest(provideSerializerMethod, new Object[] { serializer }));
    }

    private void requestSerializer(long uid) throws Exception {
        byte[] response = getResponse(new RemoteMethodCallRequest(requestSerializerMethod, new Object[] { uid }));
        Serializer serializer = (Serializer) new DeserializeStream(response).readObject();
        Repository.provideSerializer(serializer);
    }
}
