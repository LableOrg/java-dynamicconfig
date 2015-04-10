package org.lable.dynamicconfig.provider.zookeeper;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.lable.dynamicconfig.core.ConfigChangeListener;
import org.lable.dynamicconfig.core.ConfigurationException;
import org.lable.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.zookeeper.AsyncCallback.DataCallback;
import static org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * Retrieve configuration from a Zookeeper quorum, and maintain a watch for updates.
 */
public class ZookeepersAsConfigSource implements ConfigurationSource {
    private final static Logger logger = LoggerFactory.getLogger(ZookeepersAsConfigSource.class);

    /**
     * Wait this long, in seconds, before the connection attempt to the Zookeeper quorum should time out.
     */
    static int ZOOKEEPER_TIMEOUT = 10;

    String quorum;
    String znode;

    /**
     * Construct a new ZookeepersAsConfigSource.
     */
    public ZookeepersAsConfigSource() {
        // Intentionally empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "zookeeper";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> systemProperties() {
        return Arrays.asList("quorum", "znode");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This class expects two parameters:
     * <dl>
     *     <dt>quorum
     *     <dd>Comma-separated list of addresses for the Zookeeper quorum.
     *     <dt>znode
     *     <dd>Path to the configuration node.
     * </dl>
     */
    @Override
    public void configure(Configuration configuration) throws ConfigurationException {
        String quorum = configuration.getString("quorum");
        String znode = configuration.getString("znode");
        String appName = configuration.getString("appname");

        if (isBlank(quorum)) {
            throw new ConfigurationException("quorum", "No ZooKeeper quorum specified.");
        }

        if (isBlank(znode)) {
            throw new ConfigurationException("znode", "No znode specified.");
        }

        this.quorum = quorum;
        this.znode = combinePath(znode, appName);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * When this method is called, it connects to the Zookeeper quorum, and maintains a watch on the
     * configuration node.
     */
    @Override
    public void listen(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener) {
        // Act on changed data.
        DataCallback callback = new DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                if (stat != null) {
                    HierarchicalConfiguration hc = parseData(deserializer, data);
                    if (hc != null) {
                        logger.info("Configuration received from Zookeeper quorum. Znode: " + path);
                        listener.changed(hc);
                    }
                }
            }
        };

        // Launch the node watcher.
        Thread watcher = new Thread(new NodeWatcher(quorum, callback, znode));
        watcher.setName("ZooKeeper config watcher, node: " + znode);
        watcher.start();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For the initial loading of the configuration, this class connects to the Zookeeper quorum,
     * waits for a successful connection, and then loads the configuration once.
     */
    @Override
    public boolean load(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener) {
        /* It's the */
        final CountDownLatch latch = new CountDownLatch(1);

        ZooKeeper zookeeper;
        // Connect to the quorum and wait for the successful connection callback.;
        try {
            zookeeper = new ZooKeeper(quorum, ZOOKEEPER_TIMEOUT * 1000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == KeeperState.SyncConnected) {
                        // Signal that the Zookeeper connection is established.
                        latch.countDown();
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Failed to open a connection to the Zookeeper quorum: " + quorum, e);
            return false;
        }

        // Retrieve the configuration data.
        byte[] configData;
        try {
            // Wait for the connection to be established.
            boolean successfulCountDown = latch.await(ZOOKEEPER_TIMEOUT * 1000 + 300, TimeUnit.MILLISECONDS);
            if (!successfulCountDown) {
                // The latch timed out. This means a connection to the quorum could not be established.
                logger.error("Zookeeper connection attempt timed out.");
                return false;
            }
            logger.info("Looking at " + znode + " for configuration data.");
            configData = zookeeper.getData(znode, false, null);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        } catch (KeeperException e) {
            logger.error("Problem accessing configuration data through Zookeeper quorum.", e);
            return false;
        } catch (IllegalArgumentException e) {
            logger.error("Path to configuration data in Zookeeper quorum does not make sense: " + znode, e);
            return false;
        } finally {
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HierarchicalConfiguration hc = parseData(deserializer, configData);
        if (hc == null) {
            return false;
        }

        listener.changed(hc);
        return true;
    }

    /**
     * Parse the raw configuration data. This logs the data if parsing fails.
     *
     * @param raw Raw byte data.
     * @return The configuration tree, or null when parsing fails.
     */
    HierarchicalConfiguration parseData(final HierarchicalConfigurationDeserializer deserializer, byte[] raw) {
        InputStream bis = new ByteArrayInputStream(raw);
        HierarchicalConfiguration hc;
        try {
            hc = deserializer.deserialize(bis);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            logger.error("Failed to parse configuration data retrieved from Zookeeper quorum. Raw data:\n\n" +
                new String(raw) + "\n\n" + e.getCause().getMessage());
            return null;
        }
        return hc;
    }

    static String combinePath(String znode, String appName) {
        if (!isBlank(appName)) {
            if (!znode.substring(znode.length() - 1, znode.length()).equals("/")) {
                znode += "/";
            }
            if (appName.startsWith("/")) {
                appName = appName.substring(1);
            }
            znode += appName;
        }

        return znode;
    }

}
