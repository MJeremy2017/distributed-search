package management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    public static final String LEADER_ELECTION_ROOT = "/election";
    private String currentNodeName;
    private final ZooKeeper zooKeeper;
    private final ElectionCallback electionCallback;

    public LeaderElection(ZooKeeper zooKeeper, ElectionCallback electionCallback) throws InterruptedException, KeeperException {
        this.zooKeeper = zooKeeper;
        this.electionCallback = electionCallback;
        createRootZnode();
    }

    public synchronized void createRootZnode() throws InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists(LEADER_ELECTION_ROOT, false);
        if (stat == null) {
            zooKeeper.create(LEADER_ELECTION_ROOT, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.printf("Root znode %s created", LEADER_ELECTION_ROOT);
        }
    }

    public void volunteerForLeader() throws InterruptedException, KeeperException {
        // create node to leader election
        String prefix = LEADER_ELECTION_ROOT + "/c_";
        String nodeFullPath = zooKeeper.create(prefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        currentNodeName = nodeFullPath.replace(LEADER_ELECTION_ROOT + "/", "");
        System.out.println("Create election path: " + nodeFullPath + "\nCurrent node name: " + currentNodeName);
    }

    public synchronized void electLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorNode = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(LEADER_ELECTION_ROOT, false);
            Collections.sort(children);
            String leaderNode = children.get(0);
            if (currentNodeName.equals(leaderNode)) {
                System.out.println("This is the leader");
                electionCallback.onLeader();
                return;
            }

            System.out.println("This is a worker");
            int predecessorNodeIndex = children.indexOf(currentNodeName) - 1;
            predecessorNode = children.get(predecessorNodeIndex);
            System.out.println("Predecessor node:" + predecessorNode);
            predecessorStat = zooKeeper.exists(LEADER_ELECTION_ROOT + "/" + predecessorNode, this);
        }

        electionCallback.onWorker();
        System.out.println("Set watch on predecessor:" + predecessorNode);

    }


    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            try {
                electLeader();
            } catch (InterruptedException | KeeperException e) {
                e.printStackTrace();
            }
        }
    }
}
