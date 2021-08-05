package networking;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {
    private static final String STATUS_ENDPOINT = "/status";
    private final String taskEndpoint;
    private final int port;
    private final OnRequestCallback onRequestCallback;
    private HttpServer server;

    public WebServer(OnRequestCallback onRequestCallback, int port) {
        this.onRequestCallback = onRequestCallback;
        this.port = port;
        this.taskEndpoint = onRequestCallback.getTaskEndpoint();
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Start server listening on port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(taskEndpoint);

        statusContext.setHandler(this::handleStatusRequest);
        taskContext.setHandler(this::handleTaskRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        byte[] requestMessage = exchange.getRequestBody().readAllBytes();
        byte[] responseMessage = onRequestCallback.handleRequest(requestMessage);
        sendResponse(responseMessage, exchange);
    }

    private void handleStatusRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String message = "Server is alive\n";
        sendResponse(message.getBytes(), exchange);
    }

    private void sendResponse(byte[] messageBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, messageBytes.length);
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(messageBytes);
        responseBody.flush();
        responseBody.close();
    }

    public void stop() {
        server.stop(5);
    }

}
