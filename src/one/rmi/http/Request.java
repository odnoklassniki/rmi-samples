package one.rmi.http;

import one.rmi.net.Socket;

import java.net.InetSocketAddress;
import java.util.StringTokenizer;

public class Request {
    public static final int METHOD_GET  = 1;
    public static final int METHOD_HEAD = 2;
    public static final int METHOD_POST = 3;

    private int method;
    private String path;
    private String[] headers;
    private Socket socket;

    public Request(String request, Socket socket) {
        if (request.startsWith("GET ")) {
            this.method = METHOD_GET;
            this.path = request.substring(4, request.indexOf(' ', 4));
        } else if (request.startsWith("HEAD ")) {
            this.method = METHOD_HEAD;
            this.path = request.substring(5, request.indexOf(' ', 5));
        } else if (request.startsWith("POST ")) {
            this.method = METHOD_POST;
            this.path = request.substring(5, request.indexOf(' ', 5));
        } else {
            if (request.length() > 20) { request = request.substring(0, 20); }
            throw new IllegalArgumentException("Invalid HTTP method: " + request);
        }

        StringTokenizer tokenizer = new StringTokenizer(request, "\r\n", false);
        tokenizer.nextToken();
        this.headers = new String[tokenizer.countTokens()];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = tokenizer.nextToken();
        }

        this.socket = socket;
    }

    public int getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHeader(String key) {
        for (String header : headers) {
            if (header.startsWith(key)) {
                return header.substring(key.length());
            }
        }
        return null;
    }
    
    public String getParameter(String key) {
        int p = path.indexOf('?');
        if (p > 0) {
            p = path.indexOf(key, p);
            if (p > 0) {
                p += key.length();
                int q = path.indexOf('&', p);
                return q > 0 ? path.substring(p, q) : path.substring(p);
            }
        }
        return null;
    }
    
    public InetSocketAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return socket.getRemoteAddress();
    }
}
