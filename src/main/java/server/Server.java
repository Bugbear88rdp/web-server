package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int THREAD_POOL_SIZE = 64;
    private static final String CRLF = "\r\n";

    private int port;
    private ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers; // Method -> Path -> Handler
    private volatile boolean running;

    public Server() {
        this.handlers = new HashMap<>();
        this.running = false;
        initializeHandlers();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    private void initializeHandlers() {

    }

    public void listen(int port) throws IOException {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✓ Сервер запущен на порту " + port);
            System.out.println("✓ ThreadPool размер: " + THREAD_POOL_SIZE);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    threadPool.execute(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Ошибка при принятии соединения: " + e.getMessage());
                    }
                }
            }
        } finally {
            shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             InputStream input = socket.getInputStream();
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {


            Request request = parseRequest(input);

            Handler handler = findHandler(request.getMethod(), request.getPath());

            if (handler != null) {
                handler.handle(request, output);
            } else {
                sendErrorResponse(output, 404, "Not Found");
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке подключения: " + e.getMessage());
        }
    }

    private Request parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Invalid request line");
        }

        String[] parts = requestLine.split(" ");
        String method = parts[0];
        String path = parts[1];

        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim();
                String headerValue = line.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        StringBuilder body = new StringBuilder();
        int contentLength = 0;

        if (headers.containsKey("Content-Length")) {
            try {
                contentLength = Integer.parseInt(headers.get("Content-Length"));
            } catch (NumberFormatException e) {
                contentLength = 0;
            }
        }

        if (contentLength > 0) {
            char[] buffer = new char[1024];
            int charsRead;
            int totalRead = 0;

            while (totalRead < contentLength && (charsRead = reader.read(buffer)) != -1) {
                body.append(buffer, 0, charsRead);
                totalRead += charsRead;
            }
        }

        return new Request(method, path, headers, body.toString());
    }


    private Handler findHandler(String method, String path) {
        int questinMarkIndex = path.indexOf('?');
        String pathWithoutQuery = (questinMarkIndex > 0)
                ? path.substring(0, questinMarkIndex)
                : path;

        Map<String, Handler> methodHandlers = handlers.get(method);
        if  (methodHandlers != null) {
            return methodHandlers.get(pathWithoutQuery);
        }
        return null;
    }

    private void sendErrorResponse(BufferedOutputStream output, int statusCode, String statusMessage) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + CRLF +
                "Content-Type: text/plain" + CRLF +
                "Content-Length: " + statusMessage.length() + CRLF +
                CRLF +
                statusMessage;

        output.write(response.getBytes());
        output.flush();
    }

    public void shutdown() {
        this.running = false;
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
        System.out.println("✓ Сервер остановлен");
    }
}