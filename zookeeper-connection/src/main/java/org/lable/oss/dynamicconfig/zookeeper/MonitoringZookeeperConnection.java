/*
 * Copyright (C) 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.lable.oss.dynamicconfig.zookeeper.MonitoringZookeeperConnection.NodeState.WatcherState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Connects to a ZooKeeper quorum and watches a number of nodes for changes.
 */
public class MonitoringZookeeperConnection implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringZookeeperConnection.class);

    /*
     * If the Zookeeper quorum cannot be reached, the watcher thread will attempt to reconnect, with incrementally
     * longer waits between attempts. It should wait at most this long (in minutes) between attempts.
     */
    static final int MAX_RETRY_WAIT_MINUTES = 5;

    static final String LOCKING_NODES = "/locks";

    static final long MAINTENANCE_TIMER_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    final String connectString;
    final Map<String, NodeState> monitoredFiles;
    final NodeChangeListener changeListener;
    final String identityString;
    final Watcher watcher;
    final Queue<Task> jobQueue;
    final ScheduledExecutorService executor;

    CompletableFuture<Void> connectionTask;
    Future<?> jobRunner;

    ZooKeeper zooKeeper;

    State state;

    int retryCounter;
    int retryWait;

    /**
     * Create a new {@link MonitoringZookeeperConnection}.
     *
     * @param quorum         ZooKeeper quorum addresses.
     * @param chroot         Optional; limit the ZooKeeper connection to this prefix.
     * @param changeListener Callback interface called when a node changes.
     */
    public MonitoringZookeeperConnection(String[] quorum,
                                         String chroot,
                                         NodeChangeListener changeListener) {
        if (chroot == null) {
            this.connectString = String.join(",", quorum);
        } else {
            if (!chroot.startsWith("/")) chroot = "/" + chroot;
            this.connectString = String.join(",", quorum) + chroot;
        }

        logger.info("Monitoring: {}", connectString);

        this.state = State.CONNECTING;
        this.changeListener = changeListener;
        this.jobQueue = new ConcurrentLinkedQueue<>();
        this.monitoredFiles = new HashMap<>();
        this.identityString = "MonitoringZKConn " + chroot;
        this.watcher = new MZKWatcher();

        executor = Executors.newSingleThreadScheduledExecutor();
        resetRetryCounters();

        // Perform the connection to the ZooKeeper quorum asynchronously.
        connectionTask = CompletableFuture
                .runAsync(this::connect, executor)
                .thenRun(
                        () -> jobRunner = executor.scheduleWithFixedDelay(
                                new JobRunner(this), 50, 1_000, TimeUnit.MILLISECONDS
                        )
                );
    }

    /**
     * Attempt to retrieve the contents of a ZooKeeper node.
     *
     * @param node The name of the node. If this class was constructed with a chroot, then the node is
     *             relative to that chroot.
     * @return An optional containing the data as an {@link InputStream}, or empty if the node was not found.
     */
    public Optional<InputStream> load(String node) {
        if (!isLegalName(node)) {
            logger.error(identityString + ": ZooKeeper node name is not valid ({}).", node);
            return Optional.empty();
        }

        switch (state) {
            case CONNECTING:
                logger.info(identityString + ": Connection to ZooKeeper not established yet; waiting…");
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                // After waiting a little bit, try again.
                return load(node);
            case LIVE:
                // Good. Proceed with loading the content of the node.
                break;
            case CLOSED:
                return Optional.empty();
        }

        try {
            byte[] data = zooKeeper.getData(node, false, null);
            return data == null || data.length == 0
                    ? Optional.empty()
                    : Optional.of(new ByteArrayInputStream(data));
        } catch (KeeperException e) {
            logger.error(identityString + ": Failure during getData on " + node, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public void set(String node, String value, boolean createIfMissing) {
        if (!isLegalName(node)) {
            logger.error(identityString + ": ZooKeeper node name is not valid ({}).", node);
            return;
        }

        byte[] data = value.getBytes(Charset.forName("UTF-8"));
        Task task = () -> {
            try {
                zooKeeper.setData(node, data, -1);
            } catch (KeeperException.NoNodeException e) {
                if (createIfMissing) {
                    zooKeeper.create(node, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                } else {
                    logger.error("Failed to set node " + node + ", it is missing.", e);
                }
            }
        };

        jobQueue.add(task);
    }

    /**
     * Get the current state of the connection to the ZooKeeper quorum.
     *
     * @return The connection state.
     */
    public State getState() {
        return state;
    }

    /**
     * Connect to the Zookeeper quorum and create a new {@link ZooKeeper} connection instance.
     */
    synchronized void connect() {
        if (state != State.CONNECTING) return;

        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (retryCounter > 0) {
            logger.warn(identityString + ": Failed to connect to Zookeeper quorum, retrying (" + retryCounter + ").");
        }
        try {
            zooKeeper = new ZooKeeper(connectString, 3000, this.watcher);
        } catch (IOException e) {
            waitBeforeRetrying();
            connect();
        }
    }

    /**
     * Start watching a node for changes. When a change occurs, the {@link NodeChangeListener} passed to this class
     * during construction will be called.
     *
     * @param node The name of the node. If this class was constructed with a chroot, then the node is
     *             relative to that chroot.
     */
    public void listen(String node) {
        if (!isLegalName(node)) {
            logger.error("Configuration source name is not valid ({}).", node);
            return;
        }

        if (state != State.LIVE) {
            try {
                logger.info("Not connected.");
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Try again, after waiting a little while.
            listen(node);
        }

        exclusiveListen(node);
    }

    synchronized void exclusiveListen(String node) {
        NodeState nodeState;
        if (this.monitoredFiles.containsKey(node)) {
            nodeState = this.monitoredFiles.get(node);
            nodeState.monitored = true;
        } else {
            nodeState = new NodeState(node);
            this.monitoredFiles.put(node, nodeState);
        }

        attemptToSetWatcher(nodeState);
    }

    synchronized void processPart(WatchedEvent event) {
        if (state == State.CLOSED) return;

        EventType eventType = event.getType();
        String znode = event.getPath();

        // Nothing we can do here if the node is unknown.
        if (znode == null) return;

        NodeState nodeState = monitoredFiles.get(znode);
        if (nodeState == null) {
            logger.info("Watcher triggered ({}) for unknown node {}.", eventType, znode);
            return;
        }

        // Old watches of parts we are no longer interested in.
        if (!nodeState.monitored) {
            nodeState.watcherState = WatcherState.NO_WATCHER;
            return;
        }        nodeState.watcherState = WatcherState.NEEDS_WATCHER;

        switch (eventType) {
            case None:
            case NodeChildrenChanged:
                // Not relevant to us.
                break;
            case NodeDeleted:
                logger.error("Watched configuration part deleted! Keeping the last-known version in memory until this" +
                        " part is restored, or if all references to this part are removed from the config.");
                break;
            case NodeCreated:
            case NodeDataChanged:
                try {
                    byte[] data = zooKeeper.getData(znode, false, null);
                    changeListener.changed(znode, new ByteArrayInputStream(data));
                } catch (KeeperException e) {
                    logger.error(
                            "Failed to read data from znode " + znode + "! Will attempt to re-set later.",
                            e
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
        }

        attemptToSetWatcher(nodeState);
    }

    synchronized void attemptToSetWatcher(NodeState nodeState) {
        if (nodeState == null || nodeState.watcherState == WatcherState.HAS_WATCHER) return;

        try {
            zooKeeper.exists(nodeState.znode, this::processPart);
            nodeState.watcherState = WatcherState.HAS_WATCHER;
        } catch (KeeperException e) {
            logger.error("Failed to set watcher for node " + nodeState.znode + "! Will attempt to re-set later.", e);
            nodeState.watcherState = WatcherState.NEEDS_WATCHER;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void stopListening(String part) {
        if (this.monitoredFiles.containsKey(part)) {
            this.monitoredFiles.get(part).monitored = false;
        }
    }

    public ZookeeperLock prepareLock(String znode) {
        return new ZookeeperLock(zooKeeper, LOCKING_NODES + znode);
    }

    @Override
    public void close() throws IOException {
        this.state = State.CLOSED;
        if (this.connectionTask != null && !this.connectionTask.isDone()) this.connectionTask.cancel(true);
        if (this.jobRunner != null) this.jobRunner.cancel(false);

        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Reset the watchers for any nodes that lost theirs (due to IO errors etc.).
     */
    synchronized void performMaintenanceTasks() {
        this.monitoredFiles.values().stream()
                // Only monitored nodes.
                .filter(nodeState -> nodeState.monitored)
                // Of those, only nodes that need a watcher.
                .filter(nodeState -> nodeState.watcherState == WatcherState.NEEDS_WATCHER)
                // Try to (re)set a watcher.
                .forEach(nodeState -> {
                    logger.warn("Reset watcher on " + nodeState.znode);
                    attemptToSetWatcher(nodeState);
                });
    }


    /**
     * Sleep a while before continuing. This increments a waiting counter and sleeps longer each time it is
     * called, until {@link #MAX_RETRY_WAIT_MINUTES} is reached.
     */
    void waitBeforeRetrying() {
        if (retryWait < MAX_RETRY_WAIT_MINUTES * 60) {
            retryWait *= 2;
            if (retryWait > MAX_RETRY_WAIT_MINUTES * 60) {
                retryWait = MAX_RETRY_WAIT_MINUTES * 60;
            }
        }
        retryCounter++;
        try {
            logger.info("Failed to connect to ZooKeeper quorum, waiting " + retryWait + "s before retrying.");
            TimeUnit.SECONDS.sleep(retryWait);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reset retry waiting time and counter.
     */
    void resetRetryCounters() {
        retryCounter = 0;
        retryWait = 10;
    }

    static boolean isLegalName(String name) {
        return name != null && !name.isEmpty();
    }

    /**
     * State of the {@link MonitoringZookeeperConnection} class. Initially starting in state {@link #CONNECTING}, once
     * connected to the ZooKeeper quorum this class will generally be in the {@link #LIVE} state, possibly falling
     * back to the {@link #CONNECTING} state when the connection to the quorum is lost, and returning to
     * {@link #LIVE} upon reconnection. State {@link #CLOSED} is entered when this class is finally closed and cannot
     * be reopened (implying a shutdown of the application).
     */
    enum State {
        /**
         * (Re)connecting to the ZooKeeper quorum.
         */
        CONNECTING,
        /**
         * Connected to the ZooKeeper quorum.
         */
        LIVE,
        /**
         * Shutting down. Requests will be ignored.
         */
        CLOSED
    }

    /**
     * ZooKeeper node watcher.
     */
    private class MZKWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            if (state != State.LIVE && state != State.CONNECTING) return;

            Event.KeeperState state = event.getState();
            EventType type = event.getType();

            switch (state) {
                case SyncConnected:
                    // Connection (re-)established.
                    resetRetryCounters();
                    MonitoringZookeeperConnection.this.state = State.LIVE;
                    switch (type) {
                        case NodeCreated:
                            logger.info("Our configuration parent znode was created (why was it gone?).");
                            break;
                        case NodeDeleted:
                            logger.error("Our configuration parent znode was deleted! Waiting for it to be recreated…");
                            break;
                        case NodeDataChanged:
                        case NodeChildrenChanged:
                        case None:
                            break;
                    }
                    break;
                case Disconnected:
                    logger.warn("Disconnected from Zookeeper quorum, reconnecting…");
                    MonitoringZookeeperConnection.this.state = State.CONNECTING;
                    // The Zookeeper instance will automatically attempt reconnection.
                    waitBeforeRetrying();
                    break;
                case Expired:
                    logger.warn("Connection to Zookeeper quorum expired. Attempting to reconnect…");
                    MonitoringZookeeperConnection.this.state = State.CONNECTING;
                    // The Zookeeper instance is no longer valid. We have to reconnect ourselves.
                    connect();
                    return;
                case ConnectedReadOnly:
                case SaslAuthenticated:
                case AuthFailed:
                    // Probably not relevant to us.
                    break;
            }

            try {
                // Register the watcher.
                zooKeeper.getChildren("/", this);
            } catch (KeeperException.SessionExpiredException e) {
                connect();
            } catch (KeeperException e) {
                logger.error("KeeperException caught, retrying…", e);
                waitBeforeRetrying();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class JobRunner implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

        MonitoringZookeeperConnection monitoringZookeeperConnection;
        long maintenanceTaskLastRan;

        public JobRunner(MonitoringZookeeperConnection monitoringZookeeperConnection) {
            this.monitoringZookeeperConnection = monitoringZookeeperConnection;
            this.maintenanceTaskLastRan = System.currentTimeMillis();
        }

        @Override
        public void run() {
            switch (this.monitoringZookeeperConnection.getState()) {
                case CONNECTING:
                case CLOSED:
                    break;
                case LIVE:
                    while (!monitoringZookeeperConnection.jobQueue.isEmpty()) {
                        Task job = monitoringZookeeperConnection.jobQueue.poll();
                        if (job != null) {
                            try {
                                job.execute();
                            } catch (KeeperException.SessionExpiredException |
                                    KeeperException.ConnectionLossException |
                                    KeeperException.OperationTimeoutException e) {
                                // Retry later.
                                monitoringZookeeperConnection.jobQueue.add(job);
                            } catch (KeeperException e) {
                                logger.error("Unexpected ZooKeeper error.", e);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    long now = System.currentTimeMillis();
                    if (maintenanceTaskLastRan + MAINTENANCE_TIMER_INTERVAL < now) {
                        monitoringZookeeperConnection.performMaintenanceTasks();
                        maintenanceTaskLastRan = now;
                    }
                    break;
            }
        }
    }

    static class NodeState {
        private final String znode;
        boolean monitored;
        WatcherState watcherState;

        public NodeState(String znode) {
            this.znode = znode;
            this.monitored = true;
            this.watcherState = WatcherState.NO_WATCHER;
        }

        enum WatcherState {
            NEEDS_WATCHER,
            HAS_WATCHER,
            NO_WATCHER
        }
    }

    interface Task {
        void execute() throws KeeperException, InterruptedException;
    }

    @FunctionalInterface
    public interface NodeChangeListener {
        /**
         * Called when a ZooKeeper node is mutated.
         *
         * @param name        Name of the node.
         * @param inputStream Input stream containing the changed data.
         */
        void changed(String name, InputStream inputStream);
    }
}
