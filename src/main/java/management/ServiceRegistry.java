package management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Register node to zookeeper registry
 * ------------------------------------------
 * 1. upon creation, worker node will register to zookeeper cluster with its address information
 * 2. leader node will watch the registered znode and update all worker addresses
 */

public class ServiceRegistry implements Watcher {
    public static final String WORKER_REGISTRY_NAMESPACE = "/workers_service_registry";
    public static final String COORDINATOR_REGISTRY_NAMESPACE = "/coordinators_service_registry";
    private List<String> workerAddresses = null;
    private String currentWorkerNode = null;
    private final ZooKeeper zooKeeper;
    private final String SERVICE_REGISTRY_NAMESPACE;

    public ServiceRegistry(ZooKeeper zooKeeper, String registryNameSpace) throws InterruptedException, KeeperException {
        this.zooKeeper = zooKeeper;
        this.SERVICE_REGISTRY_NAMESPACE = registryNameSpace;
        createRootZnode();
    }

    public synchronized void createRootZnode() throws InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists(SERVICE_REGISTRY_NAMESPACE, false);
        if (stat == null) {
            zooKeeper.create(SERVICE_REGISTRY_NAMESPACE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.printf("root znode %s created", SERVICE_REGISTRY_NAMESPACE);
        }
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        if (currentWorkerNode != null) {
            System.out.println("Worker node already registered");
            return;
        }
        String workerPath = SERVICE_REGISTRY_NAMESPACE + "/worker_";
        currentWorkerNode = zooKeeper.create(workerPath,
                metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Worker registered to path: " + currentWorkerNode);
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if (currentWorkerNode == null)
            return;
        Stat stat = zooKeeper.exists(currentWorkerNode, false);
        if (stat == null) {
            return;
        }
        System.out.println("Unregister node:" + currentWorkerNode);
        zooKeeper.delete(currentWorkerNode, -1);
    }

    /*
    Only leader needs to call this function thus leaves a watch on the node and update all addresses upon changes
     */
    public synchronized void updateAddresses() throws InterruptedException, KeeperException {
        // set watch
        Stat stat = zooKeeper.exists(SERVICE_REGISTRY_NAMESPACE, this);
        List<String> children = zooKeeper.getChildren(SERVICE_REGISTRY_NAMESPACE, this, stat);
        List<String> addresses = new ArrayList<>(children.size());
        for (String child : children) {
            String workerFullPath = SERVICE_REGISTRY_NAMESPACE + "/" + child;
            Stat workerStat = zooKeeper.exists(workerFullPath, false);
            if (workerStat == null) {
                continue;
            }
            byte[] dataBytes = zooKeeper.getData(workerFullPath, false, workerStat);
            addresses.add(new String(dataBytes));
        }
        this.workerAddresses = Collections.unmodifiableList(addresses);
        System.out.println("Worker addresses:" + workerAddresses);

    }

    public List<String> getWorkerAddresses() {
        return workerAddresses;
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
