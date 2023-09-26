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
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.lable.oss.dynamicconfig.Precomputed;
import org.lable.oss.dynamicconfig.core.*;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.*;
import static org.lable.oss.dynamicconfig.provider.etcd.EtcdTestUtil.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EtcdAsConfigSourceIT {
    @ClassRule
    public static final EtcdTestCluster etcd = new EtcdTestCluster("test-etcd", 1);

    final static int CLUSTER_ID = 4;

    static Client client;
    static Configuration testConfig;

    @BeforeClass
    public static void setup() throws InterruptedException, ExecutionException {
        testConfig = new BaseConfiguration();
        testConfig.setProperty(
                "cluster",
                etcd.getClientEndpoints().stream().map(URI::toString).collect(Collectors.joining(","))
        );
        testConfig.setProperty("namespace", "config/");
        testConfig.setProperty(APPNAME_PROPERTY, "test");
        testConfig.setProperty(ROOTCONFIG_PROPERTY, "test");

        client = Client.builder()
                .endpoints(etcd.getClientEndpoints())
                .namespace(ByteSequence.from("config/", StandardCharsets.UTF_8))
                .build();
    }

    @Test(expected = ConfigurationException.class)
    public void testLoadNoNode() throws ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);

        // Assert that load() fails when a nonexistent node is passed as argument.
        EtcdAsConfigSource source = new EtcdAsConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("cluster", testConfig.getProperty("cluster"));
        config.setProperty("namespace", "/nope/nope");
        config.setProperty(APPNAME_PROPERTY, "nope");
        source.configure(config, new BaseConfiguration(), mockListener);

        source.load("nope");
    }

    @Test
    public void testLoad() throws Exception {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

        // Prepare the znode on the ZooKeeper.
        put(client, "test", "config:\n    string: XXX");

        EtcdAsConfigSource source = new EtcdAsConfigSource();
        source.configure(testConfig, new BaseConfiguration(), mockListener);

        source.listen("test");

        TimeUnit.MILLISECONDS.sleep(100);

        put(client, "test", "config:\n    string: YYY");

        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener).changed(eq("test"));

        ConfigurationResult config = deserializer.deserialize(new ByteArrayInputStream(get(client, "test")));

        assertThat(config.getConfiguration().getString("config.string"), is("YYY"));
    }

    @Test
    public void testListen() throws Exception {
        final String VALUE_A = "key: AAA\n";
        final String VALUE_B = "key: BBB\n";
        final String VALUE_C = "key: CCC\n";
        final String VALUE_D = "key: DDD\n";

        // Initial value. This should not be returned by the listener, but is required to make sure the node exists.
        put(client, "test", VALUE_A);

        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

        // Setup a listener to gather all returned configuration values.
        final HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        final List<String> results = new ArrayList<>();
        ConfigChangeListener listener = name -> {
            ConfigurationResult conf;
            try {
                conf = deserializer.deserialize(new ByteArrayInputStream(get(client, name)));
            } catch (ConfigurationException | ExecutionException | InterruptedException e) {
                return;
            }
            String key = conf.getConfiguration().getString("key");
            results.add(key);
        };

        EtcdAsConfigSource source = new EtcdAsConfigSource();
        source.configure(testConfig, defaults, listener);

        source.listen("test");

        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", VALUE_B);

        TimeUnit.MILLISECONDS.sleep(300);
        delete(client, "test");
        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", VALUE_C);

        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", "{BOGUS_YAML");
        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", VALUE_D);
        TimeUnit.MILLISECONDS.sleep(300);
        source.stopListening("test");
        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", VALUE_A);
        TimeUnit.MILLISECONDS.sleep(300);
        source.listen("test");
        TimeUnit.MILLISECONDS.sleep(300);
        put(client, "test", VALUE_B);
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(results.size(), is(4));
        assertThat(results.get(0), is("BBB"));
        assertThat(results.get(1), is("CCC"));
        assertThat(results.get(2), is("DDD"));
        assertThat(results.get(3), is("BBB"));

        source.close();
        TimeUnit.MILLISECONDS.sleep(300);

        put(client, "test", "{BOGUS_YAML");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(results.size(), is(4));
    }

    @Test
    public void configurationMonitorTest() throws Exception {
        put(client, "test", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "etcd");
        System.setProperty(LIBRARY_PREFIX + ".etcd.namespace", "config/");
        System.setProperty(LIBRARY_PREFIX + ".etcd.cluster", testConfig.getString("cluster"));
        System.setProperty(LIBRARY_PREFIX + "." + APPNAME_PROPERTY, "test");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        InitializedConfiguration ic = ConfigurationManager.configureFromProperties(
                defaults, new YamlDeserializer()
        );
        Configuration configuration = ic.getConfiguration();

        final AtomicInteger count = new AtomicInteger(0);
        Precomputed<String> precomputed = Precomputed.monitorByUpdate(
                configuration,
                config -> {
                    count.incrementAndGet();
                    return config.getString("key");
                }
        );

        assertThat(precomputed.get(), is("DEFAULT"));
        assertThat(count.get(), is(1));
        assertThat(configuration.getString("key"), is("DEFAULT"));

        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(precomputed.get(), is("DEFAULT"));
        assertThat(count.get(), is(1));

        put(client, "test", "key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        for (int i = 0; i < 10; i++) {
            if (configuration.getString("key").equals("DEFAULT")) {
                TimeUnit.MILLISECONDS.sleep(300);
            } else {
                break;
            }
        }


        assertThat(count.get(), is(1));
        assertThat(configuration.getString("key"), is("AAA"));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
    }

    @Test
    public void configurationMonitorIncludedTest() throws Exception {
        put(client, "test", "extends:\n    - inc.yaml\n");
        put(client, "inc.yaml", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "etcd");
        System.setProperty(LIBRARY_PREFIX + ".etcd.namespace", "config/");
        System.setProperty(LIBRARY_PREFIX + ".etcd.cluster", testConfig.getString("cluster"));
        System.setProperty(LIBRARY_PREFIX + "." + APPNAME_PROPERTY, "test");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        InitializedConfiguration ic = ConfigurationManager.configureFromProperties(
                defaults, new YamlDeserializer()
        );
        Configuration configuration = ic.getConfiguration();

        final AtomicInteger count = new AtomicInteger(0);
        Precomputed<String> precomputed = Precomputed.monitorByUpdate(
                configuration,
                config -> {
                    count.incrementAndGet();
                    return config.getString("key");
                }
        );

        assertThat(precomputed.get(), is("DEFAULT"));
        assertThat(count.get(), is(1));
        assertThat(configuration.getString("key"), is("DEFAULT"));

        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(precomputed.get(), is("DEFAULT"));
        assertThat(count.get(), is(1));

        put(client, "inc.yaml", "key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(count.get(), is(1));
        assertThat(configuration.getString("key"), is("AAA"));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
    }


    @Test
    public void viaInitializerTest() throws Exception {
        put(client, "test", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "etcd");
        System.setProperty(LIBRARY_PREFIX + ".etcd.namespace", "config/");
        System.setProperty(LIBRARY_PREFIX + ".etcd.cluster", testConfig.getString("cluster"));
        System.setProperty(LIBRARY_PREFIX + "." + APPNAME_PROPERTY, "test");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        InitializedConfiguration ic = ConfigurationManager.configureFromProperties(
                defaults, new YamlDeserializer()
        );
        Configuration configuration = ic.getConfiguration();

        assertThat(configuration.getString("key"), is("DEFAULT"));

        put(client, "test", "key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(configuration.getString("key"), is("AAA"));
    }
}
