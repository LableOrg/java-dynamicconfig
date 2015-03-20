package org.lable.dynamicconfig.provider;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.lable.dynamicconfig.core.ConfigChangeListener;
import org.lable.dynamicconfig.core.ConfigurationException;
import org.lable.dynamicconfig.core.commonsconfiguration.HierarchicalConfigurationDeserializer;
import org.lable.dynamicconfig.core.commonsconfiguration.YamlSerializerDeserializer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileBasedConfigSourceIT {
    @Test
    public void testLoad() throws URISyntaxException, InterruptedException, IOException, ConfigurationException {
        final String INPUT = "test.yml";
        URL testUrl = getClass().getResource("/" + INPUT);
        final String testYaml = testUrl.toURI().getPath();
        FileBasedConfigSource source = new FileBasedConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", testYaml);
        source.configure(config);

        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();

        final CountDownLatch latch = new CountDownLatch(1);
        Catcher catcher = new Catcher(latch);
        boolean result = source.load(deserializer, catcher);
        boolean notTimedOut = latch.await(2, TimeUnit.SECONDS);

        assertThat("Expected callback wasn't called within a reasonable time.", notTimedOut, is(true));
        assertThat(result, is(true));
        assertThat(source.config.getName(), is(INPUT));
        assertThat(catcher.caughtConfig.getString("type.unicodeString"), is("€"));
    }

    @Ignore
    @Test
    public void testListen() throws URISyntaxException, InterruptedException, IOException, ConfigurationException {
        final String VALUE_A = "key: AAA\n";
        final String VALUE_B = "key: BBB\n";
        final String VALUE_C = "key: CCC\n";

        File configFile = File.createTempFile("configuration", ".yml");
        Files.write(configFile.toPath(), VALUE_A.getBytes());

        FileBasedConfigSource source = new FileBasedConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", configFile.getPath());
        source.configure(config);
        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();


        final List<String> results = new ArrayList<>();
        source.listen(deserializer, new ConfigChangeListener() {
            @Override
            public void changed(HierarchicalConfiguration fresh) {
                results.add(fresh.getString("key"));
            }
        });

        // Sleep a little bit between file modification to ensure we have a testable sequence of events.
        TimeUnit.MILLISECONDS.sleep(200);
        // Change the contents of the file. Triggers the listener the first time.
        Files.write(configFile.toPath(), VALUE_B.getBytes());
        TimeUnit.MILLISECONDS.sleep(200);
        // Change it again. Triggers the listener a second time.
        Files.write(configFile.toPath(), VALUE_A.getBytes());
        TimeUnit.MILLISECONDS.sleep(200);
        // Remove the file. Has no impact on the listener.
        Files.delete(configFile.toPath());
        TimeUnit.MILLISECONDS.sleep(200);
        // And recreate it. Triggers the listener.
        Files.write(configFile.toPath(), VALUE_C.getBytes());
        TimeUnit.MILLISECONDS.sleep(200);

        assertThat(results.get(0), is("BBB"));
        assertThat(results.get(1), is("AAA"));
        assertThat(results.get(2), is("CCC"));
        assertThat(results.size(), is(3));
    }

    /**
     * Testing ConfigChangeListener that stores the configuration passed to the callback.
     */
    public class Catcher implements ConfigChangeListener {
        HierarchicalConfiguration caughtConfig;
        final CountDownLatch latch;

        public Catcher(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void changed(HierarchicalConfiguration fresh) {
            caughtConfig = fresh;
            latch.countDown();
        }
    }
}
