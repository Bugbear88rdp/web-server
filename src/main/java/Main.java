import server.FormPart;
import server.Handler;
import server.Request;
import server.Server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        final var server = new Server();

        setupHandlers(server);

        server.listen(9999);
    }

    private static void setupHandlers(Server server) {

        server.addHandler("GET", "/", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/index.html",
                        "text/html");
            }
        });


        server.addHandler("GET", "/styles.css", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/styles.css", "text/css");
            }
        });

        server.addHandler("GET", "/classic.html", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/classic.html", "text/html");
            }
        });

        server.addHandler("GET", "/forms.html", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/forms.html", "text/html");
            }
        });

        server.addHandler("GET", "/resources.html", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/resources.html", "text/html");
            }
        });

        server.addHandler("GET", "/links.html", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/links.html", "text/html");
            }
        });

        server.addHandler("GET", "/events.html", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/events.html", "text/html");
            }
        });

        server.addHandler("GET", "/events.js", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/events.js", "application/javascript");
            }
        });

        server.addHandler("GET", "/app.js", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                serveFile(responseStream, "src/main/resources/app.js", "application/javascript");
            }
        });

        server.addHandler("POST", "/api/messages", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                String body = request.getBody();
                String response = "{\"status\":\"ok\",\"message\":\"Message received: " + body + "\"}";

                sendJsonResponse(responseStream, 200, response);
            }
        });

        server.addHandler("GET", "/api/messages", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) throws Exception {
                String response = "[{\"id\":1,\"text\":\"Hello\"},{\"id\":2,\"text\":\"World\"}]";
                sendJsonResponse(responseStream, 200, response);
            }
        });

        server.addHandler("GET", "/search", (request, response) -> {
            String query = request.getQueryParam("q");        // "java"
            String limit = request.getQueryParam("limit");    // "10"
            List<String> tags = request.getQueryParams("tag"); // список всех tag

            String jsonResponse = "{\"q\":\"" + query +
                    "\",\"limit\":\"" + limit +
                    "\",\"tags\":" + tags + "}";

            sendJsonResponse(response, 200, jsonResponse);
        });

        server.addHandler("GET", "/api/filter", (request, response) -> {
            Map<String, List<String>> allParams = request.getAllQueryParams();

            StringBuilder json = new StringBuilder("{");
            allParams.forEach((key, values) -> {
                json.append("\"").append(key).append("\":[");
                json.append(values.stream()
                        .map(v -> "\"" + v + "\"")
                        .collect(Collectors.joining(",")));
                json.append("],");
            });
            if (json.length() > 1) {
                json.deleteCharAt(json.length() - 1); // удалить последнюю запятую
            }
            json.append("}");

            sendJsonResponse(response, 200, json.toString());
        });

        server.addHandler("POST", "/api/register", (request, response) -> {
            String name = request.getPostParam("name");
            String email = request.getPostParam("email");
            List<String> roles = request.getPostParams("role");

            String jsonResponse = "{\"name\":\"" + name +
                    "\",\"email\":\"" + email +
                    "\",\"roles\":" + roles + "}";

            sendJsonResponse(response, 201, jsonResponse);
        });

        server.addHandler("POST", "/api/upload", (request, response) -> {
            FormPart filePart = request.getFormPart("file");
            String description = request.getPostParam("description");

            if (filePart != null && filePart.isFile()) {
                String filename = filePart.getFilename();
                long fileSize = filePart.getSize();

                String jsonResponse = "{\"filename\":\"" + filename +
                        "\",\"size\":" + fileSize +
                        ",\"description\":\"" + description + "\"}";

                sendJsonResponse(response, 201, jsonResponse);
            } else {
                sendErrorResponse(response, 400, "File not provided");
            }
        });
    }


    private static void serveFile(BufferedOutputStream responseStream, String filePath, String contentType) throws Exception {
        try {
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            responseStream.write(response.getBytes());
            responseStream.write(fileContent);
            responseStream.flush();

        } catch (Exception e) {
            sendErrorResponse(responseStream, 404, "File not found: " + filePath);
        }
    }

    private static void sendJsonResponse(BufferedOutputStream responseStream, int statusCode, String jsonBody) throws Exception {
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + jsonBody.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                jsonBody;

        responseStream.write(response.getBytes());
        responseStream.flush();
    }

    private static void sendErrorResponse(BufferedOutputStream responseStream, int statusCode, String errorMessage) throws Exception {
        String response = "HTTP/1.1 " + statusCode + " Error\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + errorMessage.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                errorMessage;

        responseStream.write(response.getBytes());
        responseStream.flush();
    }
}