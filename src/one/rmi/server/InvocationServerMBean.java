package one.rmi.server;

public interface InvocationServerMBean {
    int getPoolSize();
    int getActiveCount();
    int getConnections();
}
