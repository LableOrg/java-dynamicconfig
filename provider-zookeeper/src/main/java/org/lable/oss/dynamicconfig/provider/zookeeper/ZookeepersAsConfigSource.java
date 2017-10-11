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

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationManager;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Retrieve configuration from a Zookeeper quorum, and maintain a watch for updates.
 */
public class ZookeepersAsConfigSource implements ConfigurationSource {
    private final static Logger logger = LoggerFactory.getLogger(ZookeepersAsConfigSource.class);

    String[] quorum;
    String copyQuorumTo;

    ConfigChangeListener changeListener;
    ExecutorService executorService;
    MonitoringZookeeperConnection zookeeperConnection;

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
     * <p>
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
    public void configure(Configuration configuration, Configuration defaults, ConfigChangeListener changeListener)
            throws ConfigurationException {
        String[] quorum = configuration.getStringArray("quorum");
        String znode = configuration.getString("znode");
        String copyQuorumTo = configuration.getString("copy.quorum.to");
        String rootConfig = configuration.getString(ConfigurationManager.ROOTCONFIG_PROPERTY);

        if (quorum.length == 0) {
            throw new ConfigurationException("quorum", "No ZooKeeper quorum specified.");
        }

        if (znode == null || znode.isEmpty()) {
            throw new ConfigurationException("znode", "No znode specified.");
        }

        if (rootConfig == null || rootConfig.isEmpty()) {
            throw new ConfigurationException(ConfigurationManager.APPNAME_PROPERTY, "No application name found.");
        }

        if (copyQuorumTo != null) {
            this.copyQuorumTo = copyQuorumTo;
        }

        this.changeListener = changeListener;
        this.quorum = quorum;

        defaults.setProperty(copyQuorumTo, quorum);

        zookeeperConnection = new MonitoringZookeeperConnection(quorum, znode, changeListener);
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(zookeeperConnection);
    }

    @Override
    public void listen(String name) {
        zookeeperConnection.listen(name);
    }

    @Override
    public void stopListening(String name) {
        zookeeperConnection.stopListening(name);
    }

    @Override
    public InputStream load(String name) throws ConfigurationException {
        Optional<InputStream> is = zookeeperConnection.load(name);

        if (!is.isPresent()) {
            throw new ConfigurationException("Failed to load " + name + ".");
        }

        return is.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        zookeeperConnection.close();
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
