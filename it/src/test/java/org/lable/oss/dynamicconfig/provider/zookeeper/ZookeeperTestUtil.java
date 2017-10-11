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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperTestUtil {
    /**
     * Set the testing znode's content to a value. Create the znode if necessary.
     *
     * @param value Value for the znode.
     */
    public static void setData(ZooKeeper zookeeper, String node, String value) throws Exception {
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
        stat = zookeeper.exists("/config/" + node, false);
        if (stat != null) {
            zookeeper.setData("/config/" + node, configData, -1);
        } else {
            zookeeper.create("/config/" + node, configData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public static void deleteNode(ZooKeeper zookeeper, String node) throws Exception {
        zookeeper.delete("/config/" + node, -1);
    }

    public static ZooKeeper connect(String connectString) throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        ZooKeeper zookeeper;
        // Connect to the quorum and wait for the successful connection callback.
        zookeeper = new ZooKeeper(connectString, 10000, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                // Signal that the Zookeeper connection is established.
                latch.countDown();
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

        public void shutdown() {
            super.shutdown();
        }
    }
}
