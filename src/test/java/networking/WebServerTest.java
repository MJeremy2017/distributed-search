package networking;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import search.SearchWorker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class WebServerTest {
    private static final int port = 8082;
    private static HttpClient client;
    private static final String STATUS_URL = "http://localhost:8082/status";

    @BeforeAll
    public static void setUp() {
        System.out.println("Set up...");
        SearchWorker worker = new SearchWorker();
        WebServer webServer = new WebServer(worker, port);
        webServer.startServer();
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }


    @Test
    void TestStatusEndpoint() {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(STATUS_URL))
                .build();

        CompletableFuture<Integer> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode);
        int statusCode = future.join();
        assertEquals(200, statusCode);
    }

}