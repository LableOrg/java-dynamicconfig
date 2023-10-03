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
package org.lable.oss.dynamicconfig.serialization.yaml;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationResult;
import org.lable.oss.dynamicconfig.core.IncludeReference;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;

public class YamlDeserializerTest {
    @Test
    public void testLoad() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        InputStream testYaml = getClass().getResourceAsStream("/test.yml");
        ConfigurationResult result = deserializer.deserialize(testYaml);
        HierarchicalConfiguration config = result.getConfiguration();

        // Type checking.
        assertThat(config.getString("type.string"), is("Okay"));

        List<?> list = config.getList("type.listOfStrings");
        for (Object o : list) {
            assertThat(o, instanceOf(String.class));
        }
        assertThat(list.size(), is(3));
        assertThat(list.get(0), is("One"));
        assertThat(list.get(1), is("Two"));
        assertThat(list.get(2), is("…"));

        assertThat(config.getBoolean("type.booleanFalse"), is(false));
        assertThat(config.getBoolean("type.booleanTrue"), is(true));

        list = config.getList("type.listOfIntegers");
        for (Object o : list) {
            assertThat(o, instanceOf(Integer.class));
        }
        assertThat(list.size(), is(5));
        assertThat(list.get(0), is(1));
        assertThat(list.get(4), is(-1));

        assertThat(config.getLong("type.long"), is(1000000000000L));

        // Tree model
        assertThat(config.getString("tree.branchL1a.branchL2a.branchL3a"), is("leaf_a"));
        assertThat(config.getString("tree.branchL1a.branchL2b.branchL3h"), is("leaf_h"));
        // Trim, because this value is defined as a multi-line value.
        assertThat(config.getString("tree.branchL1a.branchL2b.branchL3i").trim(), is("leaf_i"));
    }

    @Test
    public void testIncludes() throws ConfigurationException {
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        InputStream testWithIncludeTagsYaml = getClass().getResourceAsStream("/testWithIncludes.yml");

        ConfigurationResult result = deserializer.deserialize(testWithIncludeTagsYaml);

        assertThat(
                result.getIncludeReferences(),
                containsInAnyOrder(
                        new IncludeReference("tree.branch-a", "i1"),
                        new IncludeReference("base")
                )
        );
    }

    @Test(expected = ConfigurationException.class)
    public void testLoadBogusYaml() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        // This won't parse.
        deserializer.deserialize(new ByteArrayInputStream("{BOGUS_YAML".getBytes(StandardCharsets.UTF_8)));
    }
}