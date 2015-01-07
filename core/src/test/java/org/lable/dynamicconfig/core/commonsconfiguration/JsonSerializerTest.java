package org.lable.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSerializerTest {
    private static String testYaml;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void loadTestYaml() throws IOException {
        InputStream is = JsonSerializer.class.getClassLoader().getResourceAsStream("test.yml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        testYaml = writer.toString();
    }

    @Test
    public void testSave() throws ConfigurationException, IOException {
        HierarchicalConfigurationSerializer serializer = new JsonSerializer();
        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();
        HierarchicalConfiguration configuration = deserializer.deserialize(IOUtils.toInputStream(testYaml));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration, output);
        JsonNode tree = mapper.readTree(new StringReader(output.toString("UTF-8")));

        JsonNode nodeType = tree.get("type");
        assertThat(nodeType.get("unicodeString").getTextValue(), is("€"));
        assertThat(nodeType.get("booleanFalse").getBooleanValue(), is(false));
        assertThat(nodeType.get("booleanTrue").getBooleanValue(), is(true));
        ArrayNode listInt = (ArrayNode) nodeType.get("listOfIntegers");
        assertThat(listInt, instanceOf(ArrayNode.class));
    }
}
