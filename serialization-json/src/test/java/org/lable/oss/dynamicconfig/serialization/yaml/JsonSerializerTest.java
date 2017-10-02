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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSerializerTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSave() throws ConfigurationException, IOException {
        HierarchicalConfigurationSerializer serializer = new JsonSerializer();
        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        configuration.setProperty("type.unicodeString", "€");
        configuration.setProperty("type.booleanFalse", false);
        configuration.setProperty("type.booleanTrue", true);
        configuration.setProperty("type.list", Arrays.asList("1", "2", "3"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration, output, false);
        String json = IOUtils.toString(new StringReader(output.toString("UTF-8")));
        JsonNode tree = mapper.readTree(json);

        JsonNode nodeType = tree.get("type");
        assertThat(nodeType.get("unicodeString").textValue(), is("€"));
        assertThat(nodeType.get("booleanFalse").booleanValue(), is(false));
        assertThat(nodeType.get("booleanTrue").booleanValue(), is(true));
        ArrayNode listString = (ArrayNode) nodeType.get("list");
        assertThat(listString, instanceOf(ArrayNode.class));

        assertThat(json.contains("\n"), is(false));
    }

    @Test
    public void testFormatting() throws ConfigurationException, IOException {
        HierarchicalConfigurationSerializer serializer = new JsonSerializer();
        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        configuration.setProperty("type.unicodeString", "€");
        configuration.setProperty("type.booleanFalse", false);
        configuration.setProperty("type.booleanTrue", true);
        configuration.setProperty("type.list", Arrays.asList("1"));

        System.out.println(configuration.getProperty("type.list").getClass().getCanonicalName());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration, output, true);
        String json = IOUtils.toString(new StringReader(output.toString("UTF-8")));

        assertThat(json.contains("\n"), is(true));
    }
}