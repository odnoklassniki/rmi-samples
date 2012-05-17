package one.rmi.server;

class WorkerThread extends Thread {
    final byte[] inBuffer;
    final byte[] outBuffer;

    WorkerThread(Runnable r, String name, UncaughtExceptionHandler handler) {
        super(r, name);
        setUncaughtExceptionHandler(handler);
        this.inBuffer = new byte[Session.BUFFER_SIZE];
        this.outBuffer = new byte[Session.BUFFER_SIZE];
    }
}
