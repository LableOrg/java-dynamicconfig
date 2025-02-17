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
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EtcdAsConfigConnection implements ConfigurationConnection {
    private static final Logger logger = LoggerFactory.getLogger(EtcdAsConfigConnection.class);

    private final Client etcd;
    private final String namespace;
    private final ExecutorService executor;
    private final ConfigChangeListener changeListener;
    private final Map<String, Watch.Watcher> watches = new HashMap<>();

    private boolean stopped = false;

    public EtcdAsConfigConnection(String[] cluster, String namespace, ConfigChangeListener changeListener) {
        this.namespace = namespace;
        this.changeListener = changeListener;
        this.etcd = Client.builder()
                .endpoints(cluster)
                .loadBalancerPolicy("round_robin")
                .namespace(ByteSequence.from(namespace, StandardCharsets.UTF_8))
                .build();

        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void listen(String name) {
        if (stopped) return;
        if (watches.containsKey(name)) return;

        logger.info("Setting watcher on key {}.", name);
        Watch.Watcher watcher = etcd.getWatchClient().watch(nameToByteSequence(name), watchResponse -> {
            List<WatchEvent> events = watchResponse.getEvents();
            for (WatchEvent event : events) {
                KeyValue kv = event.getKeyValue();
                if (!kv.getKey().toString(StandardCharsets.UTF_8).equals(name)) continue;

                switch (event.getEventType()) {
                    case PUT:
                        executor.submit(() -> this.changeListener.changed(this, name));
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
        if (stopped) throw new ConfigurationException("Refusing to load config because this class is being shut down.");

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
    public void close() {
        this.stopped = true;
        try {
            this.watches.values().forEach(Watch.Watcher::close);
            this.executor.shutdown();

            boolean terminated = this.executor.awaitTermination(5, TimeUnit.SECONDS);
            if (!terminated) {
                logger.error("Failed to terminate the change-listeners.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        etcd.close();
    }

    static ByteSequence nameToByteSequence(String name) {
        return ByteSequence.from(name, StandardCharsets.UTF_8);
    }
}
