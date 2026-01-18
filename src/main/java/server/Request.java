package server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.CharsetUtils;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;



public class Request {
    private final String method;
    private final String path;
    private final String pathWithoutQuery;
    private final Map<String, String> headers;
    private final String body;
    private InputStream bodyStream;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> postParams;
    private final Map<String, List<FormPart>> multipartParams;

    public Request(String method, String path, Map<String, String> headers, String body, Map<String, List<FormPart>> multipartParams) {
        this.method = Objects.requireNonNull(method, "Method не может быть null");
        this.headers = new HashMap<>(headers != null ? headers : new HashMap<>());
        this.body = body != null ? body : "";
        //this.multipartParams = multipartParams;

        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex > 0) {
            this.path = path;
            this.pathWithoutQuery = path.substring(0, questionMarkIndex);
            String queryString = path.substring(questionMarkIndex + 1);
            this.queryParams = parseQueryString(queryString);
        } else {
            this.path = path;
            this.pathWithoutQuery = path;
            this.queryParams = new HashMap<>();
        }
//        this.postParams = parsePostParams();
        String contentType = this.headers.get("Content-Type");
        if (contentType != null && contentType.contains("multipart/form-data")){

            this.postParams = new HashMap<>();
            this.multipartParams = parseMultipart(contentType);
        } else {
            this.postParams = parsePostParams();
            this.multipartParams = new HashMap<>();
        }
    }

    private Map<String, List<FormPart>> parseMultipart(String contetnType) {
        Map<String, List<FormPart>> result = new HashMap<>();

        try {
            String boundary = extractBoundary(contetnType);
            if (boundary == null || body.isEmpty()) {
                return result;
            }

            List<FormPart> parts = parseMultipartBody(body, boundary);

            parts.forEach(part -> {
                result.computeIfAbsent(part.getName(), k -> new ArrayList<>()).add(part);
            });
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге multipart: " + e.getMessage());
        }
        return result;
        }

        private String extractBoundary(String contentType) {
        int boundareIndex = contentType.indexOf("boundary=");
        if (boundareIndex >= 0) {
         String boundarePart = contentType.substring(boundareIndex + 9);
         int semicolonIndex = boundarePart.indexOf(';');
         if (semicolonIndex >0) {
             return boundarePart.substring(0, semicolonIndex).trim();
         }
         return boundarePart.trim();
        }
        return null;
        }

        private List<FormPart> parseMultipartBody(String bodyStr, String boundary)
            throws IOException {
        List<FormPart> parts = new ArrayList<>();

        byte[] bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bairs = new ByteArrayInputStream(bodyBytes);

        String boundaryLine = "--" + boundary;
        String[] sections = bodyStr.split(boundaryLine);

        for (int i = 1; i < sections.length; i++) {
            String section = sections[i];

            if (section.contains("--")) {
                // Это конечная граница
                break;
            }


            FormPart part = parseFormPart(section);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts;
        }

    private FormPart parseFormPart(String sectionStr) {
        String[] parts = sectionStr.split("\r\n\r\n", 2);
        if (parts.length < 2) {
            return null;
        }

        String headers = parts[0];
        String content = parts[1];

        if (content.endsWith("\r\n")) {
            content = content.substring(0, content.length() - 2);
        }

        String name = null;
        String filename = null;
        String contentType = "text/plain";

        for (String headerLine : headers.split("\r\n")) {
            if (headerLine.toLowerCase().startsWith("content-disposition")) {
                name = extractParameter(headerLine, "name");
                filename = extractParameter(headerLine, "filename");
            } else if (headerLine.toLowerCase().startsWith("content-type")) {
                contentType = headerLine.substring(headerLine.indexOf(':') + 1).trim();
            }
        }

        if (name == null) {
            return null;
        }

        return new FormPart(name, filename, contentType, content.getBytes(StandardCharsets.UTF_8));

    }

    private String extractParameter(String line, String paramName) {
        String pattern = paramName + "=\"";
        int startIndex = line.indexOf(pattern);
        if (startIndex >= 0) {
            startIndex += pattern.length();
            int endIndex = line.indexOf('"', startIndex);
            if (endIndex > startIndex) {
                return line.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    private Map<String, List<String>> parsePostParams() {
        String contentType = headers.get("Content-Type");
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            return parseQueryString(body);
        }

        return new HashMap<>();

    }

        private Map<String, List<String>> parseQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return new HashMap<>();
        }

        List<NameValuePair> pairs = URLEncodedUtils.parse(queryString,
                Charset.forName("UTF-8"));

        return pairs.stream()
                .collect(Collectors.groupingBy(
                        NameValuePair::getName,
                        Collectors.mapping(NameValuePair::getValue,
                                Collectors.toList())
                ));
    }


    public String getMethod() {
        return method;
    }

    public String getPath() {
        return pathWithoutQuery;
    }

    public String getFullPath() {
        return path;
    }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public List<String> getQueryParams(String name) {
        return queryParams.getOrDefault(name, new ArrayList<>());
    }

    public Map<String, List<String>> getAllQueryParams() {
        return new HashMap<>(queryParams);
    }

    public String getPostParam(String name) {
        List<String> values = postParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public List<String> getPostParams(String name) {
        return postParams.getOrDefault(name, new ArrayList<>());
    }

    public Map<String, List<String>> getAllPostParams() {
        return new HashMap<>(postParams);
    }

    public FormPart getFormPart(String name) {
        List<FormPart> parts = multipartParams.get(name);
        return (parts != null && !parts.isEmpty()) ? parts.get(0) : null;
    }

    public List<FormPart> getFormParts(String name) {
        return multipartParams.getOrDefault(name, new ArrayList<>());
    }

    public Map<String, List<FormPart>> getAllFormParts() {
        return new HashMap<>(multipartParams);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public String getBody() {
        return body;
    }

    public int getContentLength() {
        String contentLength = headers.get("Content-Length");
        if (contentLength != null) {
            try {
                return Integer.parseInt(contentLength);
            } catch (NumberFormatException e) {
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
                ", path='" + pathWithoutQuery + '\'' +
                ", query=" + queryParams.size() +
                ", post=" + postParams.size() +
                ", multipart=" + multipartParams.size() +
                '}';
    }
}