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
package org.lable.oss.dynamicconfig.provider.zookeeper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.provider.zookeeper.MonitoringZookeeperConnection.NodeState.WatcherState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Connects to a ZooKeeper quorum and watches a number of nodes for changes.
 */
public class MonitoringZookeeperConnection implements Watcher, Closeable, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringZookeeperConnection.class);

    /*
     * If the Zookeeper quorum cannot be reached, the watcher thread will attempt to reconnect, with incrementally
     * longer waits between attempts. It should wait at most this long (in minutes) between attempts.
     */
    static final int MAX_RETRY_WAIT_MINUTES = 5;

    static final long MAINTENANCE_TIMER_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    final String connectString;
    final Map<String, NodeState> monitoredFiles;
    final ConfigChangeListener changeListener;
    final Timer maintenanceTimer;

    ZooKeeper zooKeeper;

    State state;

    int retryCounter;
    int retryWait;
    public MonitoringZookeeperConnection(String[] quorum, String chroot, ConfigChangeListener changeListener) {
        if (chroot == null) {
            this.connectString = String.join(",", quorum);
        } else {
            if (!chroot.startsWith("/")) chroot = "/" + chroot;
            this.connectString = String.join(",", quorum) + chroot;
        }

        logger.info("Monitoring: {}", connectString);

        this.state = State.STARTING;
        this.changeListener = changeListener;
        this.monitoredFiles = new HashMap<>();
        this.maintenanceTimer = new Timer("MonitoringZookeeperConnection clean-up timer.");

        resetRetryCounters();
    }

    public Optional<InputStream> load(String name) {
        if (!isLegalName(name)) {
            logger.error("Configuration source name is not valid ({}).", name);
            return Optional.empty();
        }

        switch (state) {
            case STARTING:
                logger.error("MonitoringZookeeperConnection was never started.");
                return Optional.empty();
            case CONNECTING:
                logger.info("Connection to ZooKeeper not established yet; waiting…");
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                return load(name);
            case LIVE:
                break;
            case CLOSED:
                return Optional.empty();
        }

        String znode = nameToZnodeName(name);

        try {
            byte[] data = zooKeeper.getData(znode, false, null);
            return data == null || data.length == 0
                    ? Optional.empty()
                    : Optional.of(new ByteArrayInputStream(data));
        } catch (KeeperException e) {
            logger.error("Failure during getData on " + name, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public void run() {
        state = State.CONNECTING;
        connect();
        maintenanceTimer.schedule(
                new MaintenanceTask(this),
                MAINTENANCE_TIMER_INTERVAL,
                MAINTENANCE_TIMER_INTERVAL
        );
    }

    public State getState() {
        return state;
    }

    /**
     * Connect to the Zookeeper quorum and create a new Zookeeper instance.
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
            logger.warn("Failed to connect to Zookeeper quorum, retrying (" + retryCounter + ").");
        }
        try {
            zooKeeper = new ZooKeeper(connectString, 3000, this);
        } catch (IOException e) {
            waitBeforeRetrying();
            connect();
        }
    }

    @Override
    public synchronized void process(WatchedEvent event) {
        if (state != State.LIVE && state != State.CONNECTING) return;

        Event.KeeperState state = event.getState();
        EventType type = event.getType();

        switch (state) {
            case SyncConnected:
            case ConnectedReadOnly:
                // Connection (re-)established.
                resetRetryCounters();
                this.state = State.LIVE;
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
                this.state = State.CONNECTING;
                // The Zookeeper instance will automatically attempt reconnection.
                waitBeforeRetrying();
                break;
            case Expired:
                logger.warn("Connection to Zookeeper quorum expired. Attempting to reconnect…");
                this.state = State.CONNECTING;
                // The Zookeeper instance is no longer valid. We have to reconnect ourselves.
                connect();
                return;
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

    public void listen(String part) {
        if (!isLegalName(part)) {
            logger.error("Configuration source name is not valid ({}).", part);
            return;
        }

        if (state != State.LIVE) {
            try {
                logger.info("Not connected.");
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            listen(part);
        }

        exclusiveListen(part);
    }

    synchronized void exclusiveListen(String part) {
        NodeState nodeState;
        if (this.monitoredFiles.containsKey(part)) {
            nodeState = this.monitoredFiles.get(part);
            nodeState.monitored = true;
        } else {
            nodeState = new NodeState(part);
            this.monitoredFiles.put(part, nodeState);
        }

        attemptToSetWatcher(nodeState);
    }

    synchronized void processPart(WatchedEvent event) {
        if (state == State.CLOSED) return;

        EventType eventType = event.getType();
        String znode = event.getPath();

        // Nothing we can do here if the node is unknown.
        if (znode == null) return;

        // Paths always start with '/'.
        String name = znodeNameToName(znode);
        NodeState nodeState = monitoredFiles.get(name);
        if (nodeState == null) {
            logger.info("Watcher triggered ({}) for unknown config part {} (znode: {}).", eventType, name, znode);
            return;
        }

        // Old watches of parts we are no longer interested in.
        if (!nodeState.monitored) {
            nodeState.watcherState = WatcherState.NO_WATCHER;
            return;
        }

        nodeState.watcherState = WatcherState.NEEDS_WATCHER;

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
                    changeListener.changed(name, new ByteArrayInputStream(data));
                } catch (KeeperException | ConfigurationException e) {
                    logger.error(
                            "Failed to read data from configuration part " + name + "! Will attempt to re-set later.",
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

    @Override
    public void close() throws IOException {
        this.state = State.CLOSED;
        maintenanceTimer.cancel();
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    synchronized void performMaintenanceTasks() {
        this.monitoredFiles.values().stream()
                .filter(nodeState -> nodeState.monitored)
                .filter(nodeState -> nodeState.watcherState == WatcherState.NEEDS_WATCHER)
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
        return name != null
                && !name.isEmpty()
                && !name.contains("--");
    }

    static String nameToZnodeName(String name) {
        if (name.startsWith("/")) name = name.substring(1);

        return "/" + name.replace("/", "--");
    }

    static String znodeNameToName(String znode) {
        return znode.substring(1).replace("--", "/");
    }

    /**
     * State of the {@link MonitoringZookeeperConnection} class. Initially starting in state {@link #STARTING}, once
     * connected to the ZooKeeper quorum this class will generally by in the {@link #LIVE} state, possibly falling
     * back to the {@link #CONNECTING} state when the connection to the quorum is lost, and returning to
     * {@link #LIVE} upon reconnection. State {@link #CLOSED} is entered when this class is finally closed.
     */
    enum State {
        /**
         * Initialisation phase.
         */
        STARTING,
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

    static class MaintenanceTask extends TimerTask {
        MonitoringZookeeperConnection connection;

        MaintenanceTask(MonitoringZookeeperConnection connection) {
            this.connection = connection;
        }
        @Override
        public void run() {
            connection.performMaintenanceTasks();
        }

    }

    static class NodeState {
        private final String znode;
        boolean monitored;
        WatcherState watcherState;

        public NodeState(String part) {
            this.znode = nameToZnodeName(part);
            this.monitored = true;
            this.watcherState = WatcherState.NO_WATCHER;
        }

        enum WatcherState {
            NEEDS_WATCHER,
            HAS_WATCHER,
            NO_WATCHER
        }
    }
}
