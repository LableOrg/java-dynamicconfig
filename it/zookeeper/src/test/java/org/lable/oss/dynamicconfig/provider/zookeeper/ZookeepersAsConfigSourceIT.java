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

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ServerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.lable.oss.dynamicconfig.Precomputed;
import org.lable.oss.dynamicconfig.core.*;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.*;
import static org.lable.oss.dynamicconfig.provider.zookeeper.ZookeeperTestUtil.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ZookeepersAsConfigSourceIT {
    private Thread server;
    private String zookeeperHost;
    private Configuration testConfig;
    private ZooKeeper zookeeper;
    private ZookeeperTestUtil.ZooKeeperThread zkServer;

    @Before
    public void setUp() throws Exception {
        final String clientPort = "21818";
        final String dataDirectory = System.getProperty("java.io.tmpdir");
        zookeeperHost = "localhost:" + clientPort;

        ServerConfig config = new ServerConfig();
        config.parse(new String[] { clientPort, dataDirectory });

        testConfig = new BaseConfiguration();
        testConfig.setProperty("quorum", zookeeperHost);
        testConfig.setProperty("znode", "/config");
        testConfig.setProperty(APPNAME_PROPERTY, "test");
        testConfig.setProperty(ROOTCONFIG_PROPERTY, "test");

        zkServer = new ZookeeperTestUtil.ZooKeeperThread(config);
        server = new Thread(zkServer);
        server.start();

        zookeeper = connect(zookeeperHost);
    }

    @Test(expected = ConfigurationException.class)
    public void testLoadNoNode() throws ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);

        // Assert that load() fails when a nonexistent node is passed as argument.
        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("quorum", zookeeperHost);
        config.setProperty("znode", "/nope/nope");
        config.setProperty(APPNAME_PROPERTY, "nope");
        source.configure(config, new BaseConfiguration(), mockListener);

        source.load("nope");
    }

    @Test
    public void testLoad() throws Exception {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        ArgumentCaptor<InputStream> argument = ArgumentCaptor.forClass(InputStream.class);

        // Prepare the znode on the ZooKeeper.
        setData(zookeeper, "test", "config:\n    string: XXX");

        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        source.configure(testConfig, new BaseConfiguration(), mockListener);

        source.listen("test");

        TimeUnit.MILLISECONDS.sleep(100);

        setData(zookeeper, "test", "config:\n    string: YYY");

        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener).changed(eq("test"), argument.capture());

        ConfigurationResult config = deserializer.deserialize(argument.getValue());

        assertThat(config.getConfiguration().getString("config.string"), is("YYY"));
    }

    @Test
    public void testListen() throws Exception {
        final String VALUE_A = "key: AAA\n";
        final String VALUE_B = "key: BBB\n";
        final String VALUE_C = "key: CCC\n";
        final String VALUE_D = "key: DDD\n";

        // Initial value. This should not be returned by the listener, but is required to make sure the node exists.
        setData(zookeeper, "test", VALUE_A);

        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

        // Setup a listener to gather all returned configuration values.
        final HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        final List<String> results = new ArrayList<>();
        ConfigChangeListener listener = (name, is) -> {
            ConfigurationResult conf;
            try {
                conf = deserializer.deserialize(is);
            } catch (ConfigurationException e) {
                return;
            }
            String key = conf.getConfiguration().getString("key");
            results.add(key);
        };

        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        source.configure(testConfig, defaults, listener);

        source.listen("test");

        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", VALUE_B);
        TimeUnit.MILLISECONDS.sleep(300);
        deleteNode(zookeeper, "test");
        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", VALUE_C);
        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", "{BOGUS_YAML");
        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", VALUE_D);
        TimeUnit.MILLISECONDS.sleep(300);
        source.stopListening("test");
        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", VALUE_A);
        TimeUnit.MILLISECONDS.sleep(300);
        source.listen("test");
        TimeUnit.MILLISECONDS.sleep(300);
        setData(zookeeper, "test", VALUE_B);
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(results.size(), is(4));
        assertThat(results.get(0), is("BBB"));
        assertThat(results.get(1), is("CCC"));
        assertThat(results.get(2), is("DDD"));
        assertThat(results.get(3), is("BBB"));

        source.close();
        TimeUnit.MILLISECONDS.sleep(300);

        setData(zookeeper, "test", "{BOGUS_YAML");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(results.size(), is(4));
    }

    @Test
    public void configurationMonitorTest() throws Exception {
        setData(zookeeper, "test", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "zookeeper");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.znode", "/config");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.quorum", zookeeperHost);
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

        setData(zookeeper, "test", "key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(count.get(), is(1));
        assertThat(configuration.getString("key"), is("AAA"));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
        assertThat(precomputed.get(), is("AAA"));
        assertThat(count.get(), is(2));
    }

    @Test
    public void configurationMonitorIncludedTest() throws Exception {
        setData(zookeeper, "test", "extends:\n    - inc.yaml\n");
        setData(zookeeper, "inc.yaml", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "zookeeper");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.znode", "/config");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.quorum", zookeeperHost);
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

        setData(zookeeper, "inc.yaml", "key: AAA");
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
        setData(zookeeper, "test", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "zookeeper");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.znode", "/config");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.quorum", zookeeperHost);
        System.setProperty(LIBRARY_PREFIX + "." + APPNAME_PROPERTY, "test");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        InitializedConfiguration ic = ConfigurationManager.configureFromProperties(
                defaults, new YamlDeserializer()
        );
        Configuration configuration = ic.getConfiguration();

        assertThat(configuration.getString("key"), is("DEFAULT"));

        setData(zookeeper, "test", "key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(configuration.getString("key"), is("AAA"));
    }

    @After
    public void tearDown() throws Exception {
        zookeeper.close();
        zkServer.shutdown();
        TimeUnit.SECONDS.sleep(1);
        server.interrupt();
    }
}
