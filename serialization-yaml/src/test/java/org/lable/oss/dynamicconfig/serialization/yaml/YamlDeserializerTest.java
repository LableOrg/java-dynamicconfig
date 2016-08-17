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
package org.lable.oss.dynamicconfig.serialization.yaml;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class YamlDeserializerTest {
    private static String testYaml;

    @BeforeClass
    public static void loadTestYaml() throws IOException {
        InputStream is = YamlDeserializer.class.getClassLoader().getResourceAsStream("test.yml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        testYaml = writer.toString();
    }

    @Test
    public void testLoad() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        HierarchicalConfiguration config = deserializer.deserialize(IOUtils.toInputStream(testYaml));

        // Type checking.
        assertThat(config.getString("type.string"), is("Okay"));

        List list = config.getList("type.listOfStrings");
        for (Object o : list) {
            assertThat(o, instanceOf(String.class));
        }
        assertThat(list.size(), is(3));
        assertThat((String) list.get(0), is("One"));
        assertThat((String) list.get(1), is("Two"));
        assertThat((String) list.get(2), is("…"));

        assertThat(config.getBoolean("type.booleanFalse"), is(false));
        assertThat(config.getBoolean("type.booleanTrue"), is(true));

        list = config.getList("type.listOfIntegers");
        for (Object o : list) {
            assertThat(o, instanceOf(Integer.class));
        }
        assertThat(list.size(), is(5));
        assertThat((Integer) list.get(0), is(1));
        assertThat((Integer) list.get(4), is(-1));

        assertThat(config.getLong("type.long"), is(1000000000000L));

        // Tree model
        assertThat(config.getString("tree.branchL1a.branchL2a.branchL3a"), is("leaf_a"));
        assertThat(config.getString("tree.branchL1a.branchL2b.branchL3h"), is("leaf_h"));
        // Trim, because this value is defined as a multi-line value.
        assertThat(config.getString("tree.branchL1a.branchL2b.branchL3i").trim(), is("leaf_i"));
    }

    @Test(expected = ConfigurationException.class)
    public void testLoadBogusYaml() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();
        // This won't parse.
        deserializer.deserialize(IOUtils.toInputStream("{BOGUS_YAML"));
    }
}