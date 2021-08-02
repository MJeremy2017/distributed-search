import management.ElectionCallback;
import management.ServiceRegistry;
import networking.WebClient;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import search.SearchCoordinator;
import search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElectionAction implements ElectionCallback {
    private ServiceRegistry workerServiceRegistry;
    private ServiceRegistry coordinatorServiceRegistry;
    private WebServer webServer = null;
    private final int port;

    public ElectionAction(ServiceRegistry workerServiceRegistry, ServiceRegistry coordinatorServiceRegistry, int port) {
        this.workerServiceRegistry = workerServiceRegistry;
        this.coordinatorServiceRegistry = coordinatorServiceRegistry;
        this.port = port;
    }

    @Override
    public void onWorker() {
        try {
            // start a worker
            SearchWorker worker = new SearchWorker();
            if (webServer == null) {
                webServer = new WebServer(worker, port);
                webServer.startServer();
            }

            String serverAddress = String.format("http://%s:%d%s",
                    InetAddress.getLocalHost().getCanonicalHostName(),
                    port,
                    worker.getTaskEndpoint());
            // register worker to cluster
            workerServiceRegistry.registerToCluster(serverAddress);
        } catch (InterruptedException | KeeperException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLeader() {
        try {
            workerServiceRegistry.unregisterFromCluster();
            workerServiceRegistry.updateAddresses();
            // start leader server
            if (webServer != null) {
                webServer.stop();
            }
            WebClient client = new WebClient();
            SearchCoordinator searchCoordinator = new SearchCoordinator(workerServiceRegistry, client);
            webServer = new WebServer(searchCoordinator, port);
            webServer.startServer();

            // register to coordinator registry
            String serverAddress = String.format("http://%s:%d%s",
                    InetAddress.getLocalHost().getCanonicalHostName(),
                    port,
                    searchCoordinator.getTaskEndpoint());
            coordinatorServiceRegistry.registerToCluster(serverAddress);

        } catch (InterruptedException | KeeperException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
