/**
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationInitializer;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
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

    String[] quorum;
    String znode;
    String copyQuorumTo;

    NodeWatcher watcher;
    ExecutorService executorService;

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
        return Arrays.asList("quorum", "znode", "copy.quorum.to");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This class required two parameters to be set in the configuration object passed:
     * <dl>
     *     <dt>quorum
     *     <dd>Comma-separated list of addresses for the Zookeeper quorum.
     *     <dt>znode
     *     <dd>Path to the configuration node.
     * </dl>
     * Additionally, the following optional parameter may be set:
     * <dl>
     *     <dt>copy.quorum.to
     *     <dd>Copy the ZooKeeper quorum to this configuration parameter to make it available in the configuration
     *     object loaded by this class.
     * </dl>
     */
    @Override
    public void configure(Configuration configuration) throws ConfigurationException {
        String[] quorum = configuration.getStringArray("quorum");
        String znode = configuration.getString("znode");
        String copyQuorumTo = configuration.getString("copy.quorum.to");
        String appName = configuration.getString(ConfigurationInitializer.APPNAME_PROPERTY);

        if (quorum.length == 0) {
            throw new ConfigurationException("quorum", "No ZooKeeper quorum specified.");
        }

        if (isBlank(znode)) {
            throw new ConfigurationException("znode", "No znode specified.");
        }

        if (isBlank(appName)) {
            throw new ConfigurationException(ConfigurationInitializer.APPNAME_PROPERTY,
                    "No application name found.");
        }

        if (isNotBlank(copyQuorumTo)) {
            this.copyQuorumTo = copyQuorumTo;
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
                    HierarchicalConfiguration hc;
                    try {
                        hc = parseData(deserializer, data);
                    } catch (ConfigurationException e) {
                        logger.error("Received invalid data from ZooKeeper quorum. I am ignoring it and keeping the " +
                                "current configuration!", e);
                        return;
                    }
                    if (hc != null) {
                        logger.info("Configuration received from Zookeeper quorum. Znode: " + path);
                        listener.changed(hc);
                    }
                }
            }
        };

        // Launch the node watcher.
        NodeWatcher watcher = new NodeWatcher(StringUtils.join(quorum, ","), callback, znode);
        this.watcher = watcher;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(watcher);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For the initial loading of the configuration, this class connects to the Zookeeper quorum,
     * waits for a successful connection, and then loads the configuration once.
     */
    @Override
    public void load(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener)
            throws ConfigurationException {
        /* It's the */
        final CountDownLatch latch = new CountDownLatch(1);

        ZooKeeper zookeeper;
        // Connect to the quorum and wait for the successful connection callback.;
        try {
            zookeeper = new ZooKeeper(StringUtils.join(quorum, ","), ZOOKEEPER_TIMEOUT * 1000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == KeeperState.SyncConnected) {
                        // Signal that the Zookeeper connection is established.
                        latch.countDown();
                    }
                }
            });
        } catch (IOException e) {
            throw new ConfigurationException("Failed to open a connection to the Zookeeper quorum: " +
                    StringUtils.join(quorum, ","), e);
        }

        // Retrieve the configuration data.
        byte[] configData;
        try {
            // Wait for the connection to be established.
            boolean successfulCountDown = latch.await(ZOOKEEPER_TIMEOUT * 1000 + 300, TimeUnit.MILLISECONDS);
            if (!successfulCountDown) {
                // The latch timed out. This means a connection to the quorum could not be established.
                throw new ConfigurationException("Zookeeper connection attempt timed out.");
            }
            logger.info("Looking at " + znode + " for configuration data.");
            configData = zookeeper.getData(znode, false, null);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ConfigurationException("Thread interrupted.");
        } catch (KeeperException e) {
            throw new ConfigurationException("Problem accessing configuration data through Zookeeper quorum.", e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Path to configuration data in Zookeeper quorum does not make sense: "
                    + znode, e);
        } finally {
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HierarchicalConfiguration hc = parseData(deserializer, configData);
        listener.changed(hc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        watcher.close();
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parse the raw configuration data. This logs the data if parsing fails.
     *
     * @param raw Raw byte data.
     * @return The configuration tree, or null when parsing fails.
     */
    HierarchicalConfiguration parseData(final HierarchicalConfigurationDeserializer deserializer, byte[] raw)
            throws ConfigurationException {
        InputStream bis = new ByteArrayInputStream(raw);
        HierarchicalConfiguration hc;
        try {
            hc = deserializer.deserialize(bis);
        } catch (ConfigurationException e) {
            throw new ConfigurationException("Failed to parse configuration data retrieved from Zookeeper quorum. " +
                    "Raw data:\n\n" + new String(raw) + "\n\n" + e.getCause().getMessage());
        }

        // If enabled, copy the Zookeeper quorum to the configuration tree.
        if (copyQuorumTo != null) {
            hc.setProperty(copyQuorumTo, quorum);
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
