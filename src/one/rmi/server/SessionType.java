package one.rmi.server;

import one.rmi.net.SocketHandle;

import java.io.IOException;

public enum SessionType {
    jboss, odkl, http;
    
    @SuppressWarnings("unchecked")
    Session newInstance(InvocationServer server, RequestProcessor requestProcessor, SocketHandle handle) throws IOException {
        switch (this) {
            case jboss:
                return new JBossSession(server, requestProcessor, handle);
            case odkl:
                return new OdklSession(server, requestProcessor, handle);
            case http:
                return new HttpSession(server, requestProcessor, handle);
        }
        return null;
    }
}
