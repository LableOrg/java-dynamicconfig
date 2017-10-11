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
import org.junit.Test;
import org.lable.oss.dynamicconfig.Precomputed;
import org.lable.oss.dynamicconfig.core.ConfigurationManager;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.APPNAME_PROPERTY;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.LIBRARY_PREFIX;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.ROOTCONFIG_PROPERTY;
import static org.lable.oss.dynamicconfig.provider.zookeeper.ZookeeperTestUtil.connect;
import static org.lable.oss.dynamicconfig.provider.zookeeper.ZookeeperTestUtil.setData;

public class ZookeeperAsConfigSourceDisruptedIT {
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

    @Test
    public void disruptedTest() throws Exception {
        setData(zookeeper, "test", "\n");

        System.setProperty(LIBRARY_PREFIX + ".type", "zookeeper");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.znode", "/config");
        System.setProperty(LIBRARY_PREFIX + ".zookeeper.quorum", zookeeperHost);
        System.setProperty(LIBRARY_PREFIX + "." + APPNAME_PROPERTY, "test");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        Configuration configuration = ConfigurationManager.configureFromProperties(
                defaults, new YamlDeserializer()
        );

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

    @After
    public void tearDown() throws Exception {
        zookeeper.close();
        zkServer.shutdown();
        TimeUnit.SECONDS.sleep(1);
        server.interrupt();
    }
}
