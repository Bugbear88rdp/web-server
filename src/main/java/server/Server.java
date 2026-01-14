package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final int THREAD_POOL_SIZE = 64;
    public static final String CRLF = "\r\n";

    private int port;
    private ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;
    private volatile boolean running;

    public Server() {
        this.handlers = new HashMap<>();
        this.running = false;
    }

    private void initializeHandlers() {

    }
}
