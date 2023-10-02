/*
 * Copyright © 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.provider.etcd;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationLoader;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Retrieve configuration from an Etcd cluster, and maintain a watch for updates.
 */
public class EtcdAsConfigSource implements ConfigurationSource {
    private String namespace;
    private String[] cluster;

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "etcd";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> systemProperties() {
        return Arrays.asList("cluster", "namespace", "copy.cluster.to");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This class required two parameters to be set in the configuration object passed:
     * <dl>
     *     <dt>cluster
     *     <dd>Comma-separated list of addresses for the Etcd cluster.
     *     <dt>namespace
     *     <dd>Key namespace.
     * </dl>
     * Additionally, the following optional parameter may be set:
     * <dl>
     *     <dt>copy.cluster.to
     *     <dd>Copy the Etcd cluster endpoints to this configuration parameter to make it available in the configuration
     *     object loaded by this class.
     * </dl>
     */
    @Override
    public void configure(Configuration configuration, Configuration defaults) throws ConfigurationException {
        String namespace = configuration.getString("namespace");
        String[] cluster = configuration.getStringArray("cluster");
        String copyClusterTo = configuration.getString("copy.cluster.to");
        String rootConfig = configuration.getString(ConfigurationLoader.ROOTCONFIG_PROPERTY);

        if (cluster.length == 0) {
            throw new ConfigurationException("cluster", "No Etcd cluster endpoints specified.");
        }

        if (namespace == null || namespace.isEmpty()) {
            throw new ConfigurationException("namespace", "No namespace specified.");
        }

        if (rootConfig == null || rootConfig.isEmpty()) {
            throw new ConfigurationException(ConfigurationLoader.APPNAME_PROPERTY, "No application name found.");
        }

        if (copyClusterTo != null) {
            defaults.setProperty(copyClusterTo, cluster);
        }

        this.namespace = namespace;
        this.cluster = cluster;
    }

    @Override
    public ConfigurationConnection connect(ConfigChangeListener changeListener) throws ConfigurationException {
        return new EtcdAsConfigConnection(cluster, namespace, changeListener);
    }
}
