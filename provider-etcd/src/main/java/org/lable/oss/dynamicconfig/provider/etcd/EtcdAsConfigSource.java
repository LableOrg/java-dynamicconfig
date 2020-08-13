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

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.watch.WatchEvent;
import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationManager;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Retrieve configuration from an Etcd cluster, and maintain a watch for updates.
 */
public class EtcdAsConfigSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(EtcdAsConfigSource.class);

    private Client etcd;
    private String namespace;
    private Map<String, Watch.Watcher> watches = new HashMap<>();
    private ConfigChangeListener changeListener;

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
    public void configure(Configuration configuration,
                          Configuration defaults,
                          ConfigChangeListener changeListener) throws ConfigurationException {
        String namespace = configuration.getString("namespace");
        String[] cluster = configuration.getStringArray("cluster");
        String copyClusterTo = configuration.getString("copy.cluster.to");
        String rootConfig = configuration.getString(ConfigurationManager.ROOTCONFIG_PROPERTY);

        if (cluster.length == 0) {
            throw new ConfigurationException("cluster", "No Etcd cluster endpoints specified.");
        }

        if (namespace == null || namespace.isEmpty()) {
            throw new ConfigurationException("namespace", "No namespace specified.");
        }

        if (rootConfig == null || rootConfig.isEmpty()) {
            throw new ConfigurationException(ConfigurationManager.APPNAME_PROPERTY, "No application name found.");
        }

        if (copyClusterTo != null) {
            defaults.setProperty(copyClusterTo, cluster);
        }

        this.namespace = namespace;
        this.changeListener = changeListener;

        this.etcd = Client.builder()
                .endpoints(cluster)
                .namespace(ByteSequence.from(namespace, StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public void listen(String name) {
        if (watches.containsKey(name)) return;

        logger.info("Setting watcher on key {}.", name);
        Watch.Watcher watcher = etcd.getWatchClient().watch(nameToByteSequence(name), watchResponse -> {
            List<WatchEvent> events = watchResponse.getEvents();
            for (WatchEvent event : events) {
                KeyValue kv = event.getKeyValue();
                if (!kv.getKey().toString(StandardCharsets.UTF_8).equals(name)) continue;

                switch (event.getEventType()) {
                    case PUT:
                        byte[] value = kv.getValue().getBytes();
                        changeListener.changed(name, new ByteArrayInputStream(value));
                        break;
                    case DELETE:
                        logger.error("Configuration key {} deleted. Waiting for it to be recreated…", name);
                        break;
                    case UNRECOGNIZED:
                    default:
                        // Ignore?
                        break;
                }
            }
        });
        watches.put(name, watcher);
    }

    @Override
    public void stopListening(String name) {
        logger.info("Removing watcher from key {}.", name);
        watches.remove(name).close();
    }

    @Override
    public InputStream load(String name) throws ConfigurationException {
        GetResponse getResponse;
        try {
            getResponse = etcd.getKVClient().get(nameToByteSequence(name)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationException(e);
        } catch (ExecutionException e) {
            throw new ConfigurationException("Failed to load key " + namespace + name + ".", e);
        }

        if (getResponse.getCount() == 0) {
            throw new ConfigurationException("No such key in etcd: " + namespace + name + ".");
        }

        return new ByteArrayInputStream(getResponse.getKvs().get(0).getValue().getBytes());
    }

    @Override
    public void close() throws IOException {
        etcd.close();
    }

    static ByteSequence nameToByteSequence(String name) {
        return ByteSequence.from(name, StandardCharsets.UTF_8);
    }
}
