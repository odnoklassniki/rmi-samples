package one.rmi.http;

import java.util.Arrays;

public class Response {
    public static final Response NO_CONTENT        = new Response("204 No Content");
    public static final Response NOT_MODIFIED      = new Response("304 Not Modified");
    public static final Response BAD_REQUEST       = new Response("400 Bad Request");
    public static final Response UNAUTHORIZED      = new Response("401 Unauthorized");
    public static final Response FORBIDDEN         = new Response("403 Forbidden");
    public static final Response NOT_FOUND         = new Response("404 Not Found");
    public static final Response INTERNAL_ERROR    = new Response("500 Internal Server Error");

    private int headerCount;
    private String[] headers;
    private byte[] body;

    public Response(String resultCode) {
        this.headerCount = 1;
        this.headers = new String[4];
        this.headers[0] = resultCode;
    }
    
    public Response(String resultCode, byte[] body) {
        this.headerCount = 2;
        this.headers = new String[4];
        this.headers[0] = resultCode;
        this.headers[1] = "Content-Length: " + body.length;
        this.body = body;
    }
    
    public static Response ok(byte[] body) {
        return new Response("200 OK", body);
    }
    
    public static Response redirect(String url) {
        Response response = new Response("302 Found");
        response.addHeader("Location: " + url);
        return response;
    }
    
    public void addHeader(String header) {
        if (headerCount == headers.length) {
            headers = Arrays.copyOf(headers, headers.length + 4);
        }
        headers[headerCount++] = header;
    }

    public int getHeaderCount() {
        return headerCount;
    }

    public String[] getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
