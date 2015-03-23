package org.lable.dynamicconfig.provider.zookeeper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lable.dynamicconfig.core.ConfigChangeListener;
import org.lable.dynamicconfig.core.ConfigurationException;
import org.lable.dynamicconfig.core.ConfigurationInitializer;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ZookeepersAsConfigSourceIT {
    private static Thread server;
    private static String zookeeperHost;
    private static Configuration testConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        final String clientPort = "21818";
        final String dataDirectory = System.getProperty("java.io.tmpdir");
        zookeeperHost = "localhost:" + clientPort;

        ServerConfig config = new ServerConfig();
        config.parse(new String[] { clientPort, dataDirectory });

        testConfig = new BaseConfiguration();
        testConfig.setProperty("quorum", zookeeperHost);
        testConfig.setProperty("znode", "/config/test");

        server = new Thread(new ZooKeeperThread(config));
        server.start();
    }

    @Test
    public void testLoadNoNode() throws ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);

        // Assert that load() fails when a nonexistent node is passed as argument.
        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("quorum", zookeeperHost);
        config.setProperty("znode", "/nope/nope");
        source.configure(config);

        boolean result = source.load(mockLoader, mockListener);

        assertThat(result, is(false));
    }

    @Test
    public void testLoad() throws Exception {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        ArgumentCaptor<HierarchicalConfiguration> argument = ArgumentCaptor.forClass(HierarchicalConfiguration.class);

        // Prepare the znode on the ZooKeeper.
        final String configValue = "config:\n" +
                "    string: XXX";
        setData(configValue);

        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        source.configure(testConfig);

        boolean result = source.load(deserializer, mockListener);

        assertThat(result, is(true));
        verify(mockListener).changed(argument.capture());
        assertThat(argument.getValue().getString("config.string"), is("XXX"));
    }

    @Test
    public void testListen() throws Exception {
        final String VALUE_A = "key: AAA\n";
        final String VALUE_B = "key: BBB\n";
        final String VALUE_C = "key: CCC\n";
        final String VALUE_D = "key: DDD\n";

        // Initial value. This should not be returned by the listener, but is required to make sure the node exists.
        setData(VALUE_A);

        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

        // Setup a listener to gather all returned configuration values.
        final List<String> results = new ArrayList<>();
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void changed(HierarchicalConfiguration fresh) {
                results.add(fresh.getString("key"));
            }
        };

        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        source.configure(testConfig);

        source.listen(deserializer, listener);

        TimeUnit.MILLISECONDS.sleep(300);
        setData(VALUE_B);
        TimeUnit.MILLISECONDS.sleep(300);
        deleteNode();
        TimeUnit.MILLISECONDS.sleep(300);
        setData(VALUE_C);
        TimeUnit.MILLISECONDS.sleep(300);
        setData("{BOGUS_YAML");
        TimeUnit.MILLISECONDS.sleep(300);
        setData(VALUE_D);
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(results.size(), is(3));
        assertThat(results.get(0), is("BBB"));
        assertThat(results.get(1), is("CCC"));
        assertThat(results.get(2), is("DDD"));
    }

    @Test
    public void viaInitializerTest() throws Exception {
        setData("\n");

        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".type", "zookeeper");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".zookeeper.znode", "/config/test");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".zookeeper.quorum", zookeeperHost);
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("key", "DEFAULT");

        Configuration configuration = ConfigurationInitializer.configureFromProperties(
                defaults, new YamlDeserializer()
        );

        assertThat(configuration.getString("key"), is("DEFAULT"));

        setData("key: AAA");
        TimeUnit.MILLISECONDS.sleep(300);

        assertThat(configuration.getString("key"), is("AAA"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.interrupt();
    }

    /**
     * Set the testing znode's content to a value. Create the znode if necessary.
     *
     * @param value Value for the znode.
     * @throws Exception Thrown when something goes wrong with the operations on the local Zookeeper.
     */
    private void setData(String value) throws Exception {
        ZooKeeper zookeeper = connect();

        // Set the configuration data.
        byte[] configData = value.getBytes();

        // There is no equivalent to `mkdir -p` in Zookeeper, so for the whole path we need to create all the nodes.
        Stat stat = zookeeper.exists("/", false);
        if (stat == null) {
            // Create the root node.
            zookeeper.create("/", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        stat = zookeeper.exists("/config", false);
        if (stat == null) {
            zookeeper.create("/config", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        stat = zookeeper.exists("/config/test", false);
        if (stat != null) {
            zookeeper.setData("/config/test", configData, -1);
        } else {
            zookeeper.create("/config/test", configData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    private void deleteNode() throws Exception {
        ZooKeeper zookeeper = connect();
        zookeeper.delete("/config/test", -1);
    }

    private ZooKeeper connect() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        ZooKeeper zookeeper;
        // Connect to the quorum and wait for the successful connection callback.
        zookeeper = new ZooKeeper(zookeeperHost, 10000, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    // Signal that the Zookeeper connection is established.
                    latch.countDown();
                }
            }
        });

        // Wait for the connection to be established.
        boolean successfulCountDown = latch.await(12, TimeUnit.SECONDS);
        if (!successfulCountDown) {
            throw new IOException("Failed to connect to local testing Zookeeper.");
        }

        return zookeeper;
    }

    /**
     * Wrap ZooKeeperServerMain in a thread, so we can start it, and later interrupt it when we are done testing.
     */
    public static class ZooKeeperThread extends ZooKeeperServerMain implements Runnable {
        private final ServerConfig config;

        public ZooKeeperThread(ServerConfig config) {
            super();
            this.config = config;
        }

        @Override
        public void run() {
            try {
                runFromConfig(config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
