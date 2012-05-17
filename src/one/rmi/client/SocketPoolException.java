package one.rmi.client;

import java.io.IOException;

public class SocketPoolException extends IOException {
    public SocketPoolException() {
    }

    public SocketPoolException(String message) {
        super(message);
    }

    public SocketPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocketPoolException(Throwable cause) {
        super(cause);
    }
}
