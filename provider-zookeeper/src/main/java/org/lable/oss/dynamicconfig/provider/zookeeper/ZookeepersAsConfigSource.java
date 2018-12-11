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
import org.lable.oss.dynamicconfig.zookeeper.MonitoringZookeeperConnection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Retrieve configuration from a Zookeeper quorum, and maintain a watch for updates.
 */
public class ZookeepersAsConfigSource implements ConfigurationSource {
    String[] quorum;
    String copyQuorumTo;

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
            defaults.setProperty(copyQuorumTo, quorum);
        }

        this.quorum = quorum;

        zookeeperConnection = new MonitoringZookeeperConnection(
                quorum,
                znode,
                (name, inputStream) -> {
                    if (!name.startsWith("/")) name = "/" + name;
                    name = znodeNameToName(name);
                    changeListener.changed(name, inputStream);
                }
        );
    }

    @Override
    public void listen(String name) {
        String znode = nameToZnodeName(name);
        zookeeperConnection.listen(znode);
    }

    @Override
    public void stopListening(String name) {
        String znode = nameToZnodeName(name);
        zookeeperConnection.stopListening(znode);
    }

    @Override
    public InputStream load(String name) throws ConfigurationException {
        String znode = nameToZnodeName(name);
        Optional<InputStream> is = zookeeperConnection.load(znode);

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
    }

    static String nameToZnodeName(String name) {
        if (name.startsWith("/")) name = name.substring(1);

        return "/" + name.replace("/", "--");
    }

    static String znodeNameToName(String znode) {
        return znode.substring(1).replace("--", "/");
    }
}
