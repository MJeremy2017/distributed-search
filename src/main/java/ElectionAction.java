import management.ElectionCallback;
import management.ServiceRegistry;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElectionAction implements ElectionCallback {
    private ServiceRegistry serviceRegistry;
    private WebServer webServer = null;
    private final int port;

    public ElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
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
            serviceRegistry.registerToCluster(serverAddress);
        } catch (InterruptedException | KeeperException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
