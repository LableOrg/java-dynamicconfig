package org.lable.dynamicconfig.serialization.yaml;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;

import java.io.*;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

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

        HierarchicalConfiguration configuration1 = deserializer.deserialize(IOUtils.toInputStream(testYaml));

        // Save the configuration tree once.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration1, output);
        final String resultOnce = output.toString("UTF-8");

        HierarchicalConfiguration configuration2 = deserializer.deserialize(IOUtils.toInputStream(resultOnce));

        // Save it twice, the output should be exactly the same as when we first saved it.
        // It won't be the same as test.yml though, because the same data can be represented
        // in more than one way.
        output = new ByteArrayOutputStream();
        serializer.serialize(configuration2, output);
        final String resultTwice = output.toString("UTF-8");

        assertThat(resultOnce, is(resultTwice));
        assertThat(resultOnce.length(), is(not(0)));
        // Verify that the data was imported correctly again on the second pass.
        assertThat(configuration1.getString("type.string"), is(configuration2.getString("type.string")));
    }

    @Test
    public void testSaveFromConf() throws ConfigurationException, IOException {
        HierarchicalConfigurationSerializer serializer = new YamlSerializer();
        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        configuration.setProperty("type.unicodeString", "€");
        configuration.setProperty("type.booleanFalse", false);
        configuration.setProperty("type.booleanTrue", true);
        configuration.setProperty("type.list", Arrays.asList("1", "2", "3"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(configuration, output);
        String yaml = IOUtils.toString(new StringReader(output.toString("UTF-8")));
//        JsonNode tree = mapper.readTree(json);
    }
}