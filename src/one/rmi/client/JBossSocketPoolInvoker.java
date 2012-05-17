package one.rmi.client;

import one.rmi.io.JBossSerializer;
import one.rmi.net.SocketInputStream;
import one.rmi.net.SocketOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;

public class JBossSocketPoolInvoker extends SocketPoolInvoker {

    static class JBossSocketWorker extends SocketWorker {
        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;

        final void writeObject(Object request) throws IOException {
            if (objectOutputStream == null) {
                objectOutputStream = JBossSerializer.createBufferedOutput(new SocketOutputStream(socket));
            }
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
        }
        
        final Object readObject() throws IOException, ClassNotFoundException {
            if (objectInputStream == null) {
                objectInputStream = JBossSerializer.createBufferedInput(new SocketInputStream(socket));
            }
            return objectInputStream.readObject();
        }
    }

    public JBossSocketPoolInvoker(ConnectionString conn) throws Exception {
        super(conn);
    }

    @Override
    protected SocketWorker createSocketWorker() {
        return new JBossSocketWorker();
    }

    @Override
    public Object invoke(Object request) throws Exception {
        JBossSocketWorker worker = (JBossSocketWorker) borrowSocketWorker();

        try {
            if (worker.isConnected()) {
                try {
                    worker.writeObject(request);
                } catch (SocketException e) {
                    // The write can fail because of a stale pooled connection - retry with a fresh one
                    worker = (JBossSocketWorker) reconnectSocketWorker(worker);
                    worker.writeObject(request);
                }
            } else {
                worker.connect(address, port, timeout);
                worker.writeObject(request);
            }

            Object result = worker.readObject();
            returnSocketWorker(worker);
            return result;
        } catch (Exception e) {
            invalidateSocketWorker(worker);
            throw e;
        }
    }
}
