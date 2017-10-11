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

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MonitoringZookeeperConnectionIT {
    @Test
    @Ignore
    public void test() throws InterruptedException {
        String[] quorum = new String[]{"tzka", "tzkb", "tzkc"};
        String chroot = "jeroen";
        MonitoringZookeeperConnection connection = new MonitoringZookeeperConnection(
                quorum,
                chroot,
                (name, inputStream) -> System.out.println(name)
        );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(connection);

        Optional<InputStream> is = connection.load("root.yaml");
        if (!is.isPresent()) fail();

        String content = new Scanner(is.get()).useDelimiter("\\A").next();

        System.out.println(content);

        connection.listen("root.yaml");
        connection.listen("inc1.yaml");

        for (int i = 0; i < 1000; i++) {
            TimeUnit.MILLISECONDS.sleep(500);
            if (i % 100 == 0) System.out.println(i);
        }

        assertThat("x", is("x"));
    }

    @Test
    @Ignore
    public void test2() throws IOException, InterruptedException, KeeperException {
        final CountDownLatch latch = new CountDownLatch(1);

        ZooKeeper zooKeeper = new ZooKeeper("tzka,tzkb,tzkc/jeroen", 1000, event -> {
            if (event.getState() == KeeperState.SyncConnected) latch.countDown();
        });

        latch.await();

        System.out.println("Connected.");

        zooKeeper.exists("/x", event -> System.out.println("1: " + event.toString()));
        zooKeeper.exists("/x", event -> System.out.println("2: " + event.toString()));

        TimeUnit.MILLISECONDS.sleep(10000);

        zooKeeper.close();

        System.out.println("Done.");
    }

    @Test
    @Ignore
    public void test3() throws IOException, InterruptedException, KeeperException {
        final CountDownLatch latch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper("tzka,tzkb,tzkc/jeroen", 1000, event -> {
            if (event.getState() == KeeperState.SyncConnected) {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);

        final CountDownLatch latch2 = new CountDownLatch(3);

        zooKeeper.exists("/test", event -> watch(zooKeeper, event, latch2));
        zooKeeper.exists("/test", event -> watch(zooKeeper, event, latch2));

        latch2.await(10, TimeUnit.MINUTES);

        System.out.println("DONE");

        zooKeeper.close();
    }

    private void watch(ZooKeeper zooKeeper, WatchedEvent event, CountDownLatch latch) {
        System.out.println(event.getType());
        latch.countDown();
        try {
            zooKeeper.exists("/test", e -> watch(zooKeeper, e, latch));
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}