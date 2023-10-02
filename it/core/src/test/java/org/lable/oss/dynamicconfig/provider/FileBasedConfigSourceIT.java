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
package org.lable.oss.dynamicconfig.provider;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationResult;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlSerializer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.lable.oss.dynamicconfig.core.ConfigurationLoader.ROOTCONFIG_PROPERTY;

public class FileBasedConfigSourceIT {
    @Test
    public void testLoad() throws URISyntaxException, IOException, ConfigurationException {
        final String INPUT = "test.yml";
        URL testUrl = getClass().getResource("/" + INPUT);
        final String testYaml = testUrl.toURI().getPath();
        FileBasedConfigSource source = new FileBasedConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty(ROOTCONFIG_PROPERTY, testYaml);
        source.configure(config, new HierarchicalConfiguration());
        HierarchicalConfigurationDeserializer deserializer;

        try (ConfigurationConnection connection = source.connect(null)) {
            deserializer = new YamlDeserializer();

            InputStream is = connection.load("test.yml");

            ConfigurationResult result = deserializer.deserialize(is);
            Configuration configuration = result.getConfiguration();
            assertThat(configuration.getString("type.unicodeString"), is("€"));
        }
    }

    @Test
    public void testMultiFileLoad() throws Exception {
        final String INPUT = "multiple-files/root.yaml";
        URL testUrl = getClass().getResource("/" + INPUT);
        final String testYaml = testUrl.toURI().getPath();
        FileBasedConfigSource source = new FileBasedConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty(ROOTCONFIG_PROPERTY, testYaml);
        source.configure(config, new HierarchicalConfiguration());

        try (ConfigurationConnection connection = source.connect(null)) {
            HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

            InputStream is = connection.load("root.yaml");

            ConfigurationResult result = deserializer.deserialize(is);
            HierarchicalConfiguration configuration = result.getConfiguration();

            YamlSerializer serializer = new YamlSerializer();

            serializer.serialize(configuration, System.out);
        }

//        assertThat(configuration.getString("type.unicodeString"), is("€"));
    }

//    @Ignore
//    @Test
//    public void testListen() throws URISyntaxException, InterruptedException, IOException, ConfigurationException {
//        final String VALUE_A = "key: AAA\n";
//        final String VALUE_B = "key: BBB\n";
//        final String VALUE_C = "key: CCC\n";
//
//        File configFile = File.createTempFile("configuration", ".yml");
//        Files.write(configFile.toPath(), VALUE_A.getBytes());
//
//        FileBasedConfigSource source = new FileBasedConfigSource();
//        Configuration config = new BaseConfiguration();
//        config.setProperty("path", configFile.getPath());
//        source.configure(config);
//        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
//
//
//        final List<String> results = new ArrayList<>();
//        source.listen(deserializer, fresh -> results.add(fresh.getString("key")));
//
//        // Sleep a little bit between file modification to ensure we have a testable sequence of events.
//        TimeUnit.MILLISECONDS.sleep(200);
//        // Change the contents of the file. Triggers the listener the first time.
//        Files.write(configFile.toPath(), VALUE_B.getBytes());
//        TimeUnit.MILLISECONDS.sleep(200);
//        // Change it again. Triggers the listener a second time.
//        Files.write(configFile.toPath(), VALUE_A.getBytes());
//        TimeUnit.MILLISECONDS.sleep(200);
//        // Remove the file. Has no impact on the listener.
//        Files.delete(configFile.toPath());
//        TimeUnit.MILLISECONDS.sleep(200);
//        // And recreate it. Triggers the listener.
//        Files.write(configFile.toPath(), VALUE_C.getBytes());
//        TimeUnit.MILLISECONDS.sleep(200);
//
//        assertThat(results.get(0), is("BBB"));
//        assertThat(results.get(1), is("AAA"));
//        assertThat(results.get(2), is("CCC"));
//        assertThat(results.size(), is(3));
//    }
//
//    /**
//     * Testing ConfigChangeListener that stores the configuration passed to the callback.
//     */
//    public class Catcher implements ConfigChangeListener {
//        HierarchicalConfiguration caughtConfig;
//        final CountDownLatch latch;
//
//        public Catcher(CountDownLatch latch) {
//            this.latch = latch;
//        }
//
//        @Override
//        public void changed(HierarchicalConfiguration fresh) {
//            caughtConfig = fresh;
//            latch.countDown();
//        }
//    }
}
