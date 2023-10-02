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
package org.lable.oss.dynamicconfig.core;


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

public class ConcurrencyIT {
    @Test
    public void concurrencyTest() throws IOException, InterruptedException, ConfigurationException {
        final int threadCount = 20;

        final Path configFile = Files.createTempFile("config", ".yaml");
        Files.write(configFile, "test: 0\n".getBytes());

        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".type", "file");
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".rootconfig", configFile.toAbsolutePath().toString());
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("test", -1);
        final ConfigurationManager ic = ConfigurationLoader.configureFromProperties(
                defaults, new YamlDeserializer()
        );
        final Configuration configuration = ic.getConfiguration();

        final CountDownLatch ready = new CountDownLatch(threadCount + 1);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threadCount + 1);
        final Map<Integer, Long> result = new ConcurrentHashMap<>(threadCount);
        final long stopTime = System.currentTimeMillis() + 1_000;

        for (int i = 0; i < threadCount; i++) {
            final Integer number = 10 + i;
            new Thread(() -> {
                ready.countDown();
                try {
                    result.put(number, 0L);
                    start.await();
                    while (System.currentTimeMillis() < stopTime) {
                        System.out.println(configuration.getLong("test"));
                        result.put(number, result.get(number) + 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                done.countDown();
            }, String.valueOf(number)).start();
        }

        new Thread(() -> {
            long count = 1;
            ready.countDown();
            try {
                start.await();
                while (System.currentTimeMillis() < stopTime) {
                    String contents = "test: " + count + "\n";
                    Files.write(configFile, contents.getBytes());
                    count++;
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
            done.countDown();
        }, "setter").start();

        ready.await();
        start.countDown();
        done.await();

        for (Map.Entry<Integer, Long> entry : result.entrySet()) {
            System.out.println("Thread " + entry.getKey() + ": " + entry.getValue());
        }

    }
}
