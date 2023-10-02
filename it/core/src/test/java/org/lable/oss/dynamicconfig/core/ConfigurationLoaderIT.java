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

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationComposition.ConfigReference;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;
import org.lable.oss.dynamicconfig.provider.FileBasedConfigSource;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlSerializer;
import org.lable.oss.dynamicconfig.test.FileUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationLoaderIT {
    @Test
    public void testMultipleFilesConfig() throws ConfigurationException, URISyntaxException {
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("outer.def-a", 111);
        defaults.setProperty("outer.def-b", 222);
        defaults.setProperty("outer.branch-a.x", "x");

        final String INPUT = "multiple-files/root.yaml";
        URL testUrl = getClass().getResource("/" + INPUT);
        final String testYaml = testUrl.toURI().getPath();
        Configuration sourceConfiguration = new BaseConfiguration();
        ConfigurationManager ic = ConfigurationLoader.fromTheseSettings(
                testYaml,
                "app",
                new FileBasedConfigSource(),
                new YamlDeserializer(),
                sourceConfiguration,
                defaults
        );
        Configuration configuration = ic.getConfiguration();

        // Overridden default.
        assertThat(configuration.getInt("outer.def-a"), is(123));

        // Test basic include.
        assertThat(configuration.getInt("outer.branch-a.a1"), is(1));
        assertThat(configuration.getInt("outer.branch-a.a2"), is(2));

        // Included config should override the extended one.
        assertThat(configuration.getList("outer.branch-a.lst"), contains(4, 5, 6));

        // Include in an include.
        assertThat(configuration.getInt("outer.branch-a.sub.s1"), is(1234));
        assertThat(configuration.getList("outer.branch-a.sub.s2"), contains("a", "b"));

        // Include in an extended config.
        assertThat(configuration.getInt("outer.branch-c.sub.s1"), is(1234));
        assertThat(configuration.getList("outer.branch-c.sub.s2"), contains("a", "b"));

        // Inherited from default.
        assertThat(configuration.getInt("outer.def-b"), is(222));
    }

    @Test
    public void testFileMutations() throws IOException, ConfigurationException, InterruptedException {
        Path dir = Files.createTempDirectory("filemutationsit");

        Files.copy(getClass().getResourceAsStream("/multiple-files/root.yaml"), dir.resolve("root.yaml"));
        Files.copy(getClass().getResourceAsStream("/multiple-files/base1.yaml"), dir.resolve("base1.yaml"));
        Files.copy(getClass().getResourceAsStream("/multiple-files/inc1.yaml"), dir.resolve("inc1.yaml"));
        Files.copy(getClass().getResourceAsStream("/multiple-files/inc2.yaml"), dir.resolve("inc2.yaml"));

        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("outer.def-a", 111);
        defaults.setProperty("outer.def-b", 222);
        defaults.setProperty("outer.branch-a.x", "x");

        Configuration sourceConfiguration = new BaseConfiguration();
        ConfigurationManager ic = ConfigurationLoader.fromTheseSettings(
                dir.resolve("root.yaml").toString(),
                "app",
                new FileBasedConfigSource(),
                new YamlDeserializer(),
                sourceConfiguration,
                defaults
        );

        Configuration configuration = ic.getConfiguration();

        assertThat(configuration.getString("outer.branch-a.z"), is(nullValue()));

        TimeUnit.SECONDS.sleep(2);

        Files.copy(
                getClass().getResourceAsStream("/multiple-files/base1-alt1.yaml"),
                dir.resolve("base1.yaml"),
                StandardCopyOption.REPLACE_EXISTING
        );

        TimeUnit.SECONDS.sleep(2);

        assertThat(configuration.getString("outer.branch-a.z"), is("z"));

        Files.delete(dir.resolve("root.yaml"));

        TimeUnit.SECONDS.sleep(2);

        assertThat(configuration.getString("outer.branch-a.z"), is("z"));
        assertThat(configuration.getInt("outer.def-a"), is(123));

        Files.copy(getClass().getResourceAsStream("/multiple-files/root.yaml"), dir.resolve("root.yaml"));

        TimeUnit.SECONDS.sleep(2);

        assertThat(configuration.getString("outer.branch-a.z"), is("z"));
        assertThat(configuration.getInt("outer.def-a"), is(123));

        FileUtil.deleteDirAndContents(dir);
    }

    public void printConfig(ConcurrentConfiguration configuration) {
        YamlSerializer serializer = new YamlSerializer();

        ConfigurationLoader.composition.allReferences.keySet().forEach(
                s -> {
                    ConfigReference ref = ConfigurationLoader.composition.allReferences.get(s);
                    try {
                        serializer.serialize(ref.configuration, System.out);
                        System.out.println("---");
                    } catch (ConfigurationException e) {
                        e.printStackTrace();
                    }
                }
        );

        configuration.withConfiguration(combinedConfiguration -> {
            try {
                serializer.serialize(combinedConfiguration, System.out);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        });
    }
}