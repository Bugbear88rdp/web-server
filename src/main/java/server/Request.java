package server;

import javax.swing.text.AbstractDocument;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;

    public Request(String method,
                   String path,
                   Map<String, String> headers,
                   String body) {
        this.method = Objects.requireNonNull(method, "Method can't be null");
        this.path = Objects.requireNonNull(path, "Path can't be null");
        this.headers = new HashMap<>(headers != null ? headers : new HashMap<>());
        this.body = body != null ? body : "";
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public String body() {
        return body;
    }

    public int getContentLength() {
        String contentetLength = headers.get("Conetent-Length");
        if (contentetLength != null) {
            try {
                return Integer.parseInt(contentetLength);
            } catch (NumberFormatException e){
                return 0;
            }
        }
        return 0;
    }

    public String getContentType() {
        return headers.get("Content-Type");
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", bodyLength=" + body.length() +
                '}';
    }
}
