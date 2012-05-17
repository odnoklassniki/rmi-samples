package one.rmi.server;

import one.rmi.io.JBossSerializer;
import one.rmi.net.Socket;
import one.rmi.net.SocketHandle;
import one.rmi.net.SocketInputStream;
import one.rmi.net.SocketOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class JBossSession extends Session<Object, Object> {
    private ObjectInputStream in;
    private ObjectOutputStream out;

    JBossSession(InvocationServer server, RequestProcessor<Object, Object> requestProcessor, SocketHandle handle) {
        super(server, requestProcessor, handle);
    }

    private void createStreams() throws IOException {
        Socket socket = handle.socket();
        out = JBossSerializer.createBufferedOutput(new SocketOutputStream(socket));
        in = JBossSerializer.createBufferedInput(new SocketInputStream(socket));
    }
    
    @Override
    public void run() {
        try {
            if (in == null) {
                createStreams();
            }
            Object request = in.readObject();
            Object result = requestProcessor.process(request);
            out.writeObject(result);
            out.flush();
            handle.renew();
        } catch (Throwable e) {
            reportException(e);
            close();
        }
    }
}
