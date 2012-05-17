package one.rmi.server;

public interface RequestProcessor<Q, R> {
    R process(Q request) throws Exception;
}
