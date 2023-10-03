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
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationResult;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class YamlSerializerTest {
    private static String testYaml;

    @BeforeClass
    public static void loadTestYaml() throws IOException {
        InputStream is = YamlDeserializer.class.getClassLoader().getResourceAsStream("test.yml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        testYaml = writer.toString();
    }

    @Test
    public void testSave() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationSerializer serializer = new YamlSerializer();
        HierarchicalConfigurationDeserializer deserializer = new YamlDeserializer();

        ConfigurationResult result1 = deserializer.deserialize(new ByteArrayInputStream(testYaml.getBytes(StandardCharsets.UTF_8)));
        HierarchicalConfiguration configuration1 = result1.getConfiguration();

        // Save the configuration tree once.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration1, output);
        final String resultOnce = output.toString(StandardCharsets.UTF_8);

        ConfigurationResult result2 = deserializer.deserialize(
                new ByteArrayInputStream(resultOnce.getBytes(StandardCharsets.UTF_8))
        );
        HierarchicalConfiguration configuration2 = result2.getConfiguration();

        // Save it twice, the output should be exactly the same as when we first saved it.
        // It won't be the same as test.yml though, because the same data can be represented
        // in more than one way.
        output = new ByteArrayOutputStream();
        serializer.serialize(configuration2, output);
        final String resultTwice = output.toString(StandardCharsets.UTF_8);

        assertThat(resultOnce, is(resultTwice));
        assertThat(resultOnce.length(), is(not(0)));
        // Verify that the data was imported correctly again on the second pass.
        assertThat(configuration1.getString("type.string"), is(configuration2.getString("type.string")));
    }
}