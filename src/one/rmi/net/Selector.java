package one.rmi.net;

import java.util.Iterator;

public interface Selector extends Iterable<SocketHandle> {
    SocketHandle register(Socket socket);
    Iterator<SocketHandle> iterator();
    Iterator<SocketHandle> select();
    int size();
    void close();
}
