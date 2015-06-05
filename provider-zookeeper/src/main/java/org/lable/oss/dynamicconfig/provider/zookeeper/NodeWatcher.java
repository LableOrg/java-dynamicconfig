package org.lable.oss.dynamicconfig.provider.zookeeper;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper node watcher.
 */
public class NodeWatcher implements Watcher, Runnable {
    private final static Logger logger = LoggerFactory.getLogger(NodeWatcher.class);

    /*
     * If the Zookeeper quorum cannot be reached, the watcher thread will attempt to reconnect, with incrementally
     * longer waits between attempts. It should wait at most this long (in minutes) between attempts.
     */
    static final int MAX_RETRY_WAIT_MINUTES = 5;

    private final AsyncCallback.DataCallback callback;
    private final String quorum;
    private final String path;

    private ZooKeeper zk;

    private int retryCounter;
    private int retryWait;

    /**
     * Construct a new NodeWatcher.
     *
     * @param quorum Comma-separated list of addresses for the Zookeeper quorum.
     * @param callback Callback method called upon changes in the node.
     * @param path Path of the znode monitored.
     */
    public NodeWatcher(String quorum, AsyncCallback.DataCallback callback, String path) {
        this.quorum = quorum;
        this.callback = callback;
        this.path = path;
        resetRetryCounters();
        connect();
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ie) {
            // Exit.
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        Event.KeeperState state = watchedEvent.getState();
        Event.EventType type = watchedEvent.getType();

        // This switch handles all relevant states, and tries to reset the watch on the znode after it is
        // triggered.
        switch (state) {
            case SyncConnected:
            case ConnectedReadOnly:
                resetRetryCounters();
                switch (type) {
                    case NodeCreated:
                    case NodeDataChanged:
                        // Configuration znode changed, let the callback know.
                        zk.getData(path, this, callback, null);
                        break;
                    case None:
                        registerWatcher(path);
                        break;
                    case NodeDeleted:
                        logger.error("Our configuration znode was deleted. Waiting for it to be recreated…");
                        registerWatcher(path);
                        break;
                }
                break;
            case Disconnected:
                logger.warn("Disconnected from Zookeeper quorum, reconnecting…");
                // The Zookeeper instance will automatically attempt reconnection.
                waitBeforeRetrying();
                break;
            case Expired:
                logger.warn("Connection to Zookeeper quorum expired. Attempting to reconnect…");
                // The Zookeeper instance is no longer valid. We have to reconnect ourselves.
                connect();
                break;
            case SaslAuthenticated:
            case AuthFailed:
                // Probably not relevant to us.
                break;
        }
    }

    /**
     * Connect to the Zookeeper quorum and create a new Zookeeper instance.
     */
    void connect() {
        logger.debug("Connecting to ZooKeeper Quorum.");
        if (zk != null) {
            try {
                zk.close();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (retryCounter > 0) {
            logger.warn("Failed to connect to Zookeeper quorum, retrying (" + retryCounter + ").");
        }
        try {
            zk = new ZooKeeper(quorum, 3000, this);
        } catch (IOException e) {
            waitBeforeRetrying();
            connect();
        }
    }

    /**
     * Register the ZooKeeper watcher for a node. If registering it fails {@link #waitBeforeRetrying()} is called
     * once to prevent hammering.
     *
     * @param path Node to watch.
     */
    void registerWatcher(String path) {
        try {
            // Register the watcher.
            zk.exists(path, this);
        } catch (KeeperException.SessionExpiredException e) {
            connect();
        } catch (KeeperException e) {
            logger.error("KeeperException caught, retrying…", e);
            waitBeforeRetrying();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
}
