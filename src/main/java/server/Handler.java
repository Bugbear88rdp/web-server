package server;

import com.sun.net.httpserver.Request;

import java.io.BufferedInputStream;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedInputStream responseStream) throws Exception;
}
