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
package org.lable.oss.dynamicconfig.zookeeper;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class ZooKeeperLockIT {
    @Ignore
    @Test
    public void test() throws InterruptedException {
        MonitoringZookeeperConnection monitoringZookeeperConnection =
                new MonitoringZookeeperConnection(
                        new String[]{"tzka", "tzkb", "tzkc"},
                        "test",
                        (name, inputStream) -> System.out.println(name)
                );

        final CountDownLatch connected = new CountDownLatch(1);

        monitoringZookeeperConnection.registerObserver(new ZooKeeperConnectionObserver() {
            @Override
            public void disconnected() {

            }

            @Override
            public void connected() {
                connected.countDown();
            }
        });

        if (monitoringZookeeperConnection.state != MonitoringZookeeperConnection.State.LIVE) {
            connected.await();
        }

        System.out.println("Connected!");

        ExecutorService threadPool = Executors.newFixedThreadPool(20);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch ready = new CountDownLatch(20);
        final CountDownLatch done = new CountDownLatch(20);

        for (int i = 0; i < 20; i++) {
            threadPool.submit(() -> {
                try {
                    long id = Thread.currentThread().getId();
                    ready.countDown();
                    latch.await();

                    System.out.println("Start -> " + id);
                    Lock lock = monitoringZookeeperConnection.prepareLock("/one");

                    if (id % 2 == 0) {
                        lock.lock();
                    } else {
                        boolean success = lock.tryLock(10, TimeUnit.SECONDS);
                        if (!success) {
                            System.out.println("No lock -> " + id);
                            return;
                        }
                    }

                    System.out.println("Got lock -> " + id);

                    TimeUnit.SECONDS.sleep(4);

                    lock.unlock();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        latch.countDown();
        done.await();

        System.out.println("Done.");
    }
}
