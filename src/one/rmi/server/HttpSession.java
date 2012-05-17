package one.rmi.server;

import one.rmi.http.Request;
import one.rmi.http.Response;
import one.rmi.io.ByteArrayStream;
import one.rmi.io.HTTPUtils;
import one.rmi.net.Socket;
import one.rmi.io.UTFUtils;
import one.rmi.net.SocketHandle;

import java.io.IOException;

class HttpSession extends Session<Request, Response> {
    private static final byte[] RESPONSE_PROTOCOL = UTFUtils.toBytes("HTTP/1.0 ");

    private final Socket socket;

    HttpSession(InvocationServer server, RequestProcessor<Request, Response> processor, SocketHandle handle) throws IOException {
        super(server, processor, handle);
        this.socket = handle.socket();
        this.lastAccessTime = System.currentTimeMillis();
    }

    private Response processRequest(Request request) throws Exception {
        try {
            return requestProcessor.process(request);
        } catch (Exception e) {
            writeResponse(Response.INTERNAL_ERROR, false);
            throw e;
        }
    }

    private void writeResponse(Response response, boolean headOnly) throws IOException {
        ByteArrayStream bas = new ByteArrayStream(getOutBuffer());
        bas.write(RESPONSE_PROTOCOL);
        
        int headerCount = response.getHeaderCount();
        String[] headers = response.getHeaders();
        for (int i = 0; i < headerCount; i++) {
            bas.writeBytes(headers[i]);
            bas.write('\r'); bas.write('\n');
        }
        bas.write('\r'); bas.write('\n');

        byte[] body = response.getBody();
        if (body == null || headOnly) {
            socket.write(bas.array(), 0, bas.count());
        } else if (body.length <= bas.available()) {
            bas.write(body);
            socket.write(bas.array(), 0, bas.count());
        } else {
            socket.write(bas.array(), 0, bas.count());
            socket.write(body);
        }
    }

    @Override
    public void run() {
        try {
            lastAccessTime = 0;
            byte[] buf = getInBuffer();

            for (int start = 0, end = 0; ; ) {
                int headerEnd = HTTPUtils.findHeaderEnd(buf, start, end);
                if (headerEnd >= 0) {
                    Request request;
                    try {
                        request = new Request(UTFUtils.read(buf, 0, headerEnd), socket);
                    } catch (IllegalArgumentException e) {
                        writeResponse(Response.BAD_REQUEST, false);
                        break;
                    }

                    Response response = processRequest(request);
                    end -= headerEnd;

                    if (response.getBody() != null && "Keep-Alive".equalsIgnoreCase(request.getHeader("Connection: "))) {
                        response.addHeader("Connection: Keep-Alive");
                        writeResponse(response, request.getMethod() == Request.METHOD_HEAD);
                        if (end > 0) {
                            System.arraycopy(buf, headerEnd, buf, 0, end);
                            start = 0;
                            continue;
                        }
                        lastAccessTime = System.currentTimeMillis();
                        handle.renew();
                    } else {
                        writeResponse(response, request.getMethod() == Request.METHOD_HEAD);
                        close();
                    }
                    break;
                }
                start = end;
                end += socket.read(buf, end, BUFFER_SIZE - end);
            }
        } catch (Throwable e) {
            reportException(e);
            close();
        }
    }
}
