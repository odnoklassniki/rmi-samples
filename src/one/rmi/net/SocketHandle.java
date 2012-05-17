package one.rmi.net;

public abstract class SocketHandle {
    protected Socket socket;
    protected Object attachment;

    protected SocketHandle(Socket socket) {
        this.socket = socket;
    }

    protected SocketHandle(Socket socket, Object attachment) {
        this.socket = socket;
        this.attachment = attachment;
    }

    public final Socket socket() {
        return socket;
    }

    public final Object attachment() {
        return attachment;
    }
    
    public final void attach(Object attachment) {
        this.attachment = attachment;
    }

    public abstract void activate();
    public abstract void renew();
    public abstract void cancel();
    public abstract boolean isReadable();
    public abstract boolean isWritable();

    @Override
    public String toString() {
        return "SocketHandle[socket=" + socket.fd + ",attachment=" + attachment + "]";
    }
}
