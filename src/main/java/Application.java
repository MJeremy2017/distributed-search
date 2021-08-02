import management.LeaderElection;
import management.ServiceRegistry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Application implements Watcher {
    private static final String ZOOKEEPER_HOST = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;

    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int port = args.length == 1? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Application app = new Application();
        ZooKeeper zooKeeper = app.connectToZookeeper();

        ServiceRegistry workerServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.WORKER_REGISTRY_NAMESPACE);
        ServiceRegistry coordinatorServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.COORDINATOR_REGISTRY_NAMESPACE);

        ElectionAction electionAction = new ElectionAction(workerServiceRegistry, coordinatorServiceRegistry, port);

        // the major starting point
        LeaderElection leaderElection = new LeaderElection(zooKeeper, electionAction);
        leaderElection.volunteerForLeader();
        leaderElection.electLeader();

        app.run();
        app.close();
    }

    public ZooKeeper connectToZookeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZOOKEEPER_HOST, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.close();
            System.out.println("Server closed");
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected -> System.out.println("Connection established to " + ZOOKEEPER_HOST);
                default -> {
                    synchronized (zooKeeper) {
                        zooKeeper.notifyAll();
                    }
                }
            }
        }
    }
}
