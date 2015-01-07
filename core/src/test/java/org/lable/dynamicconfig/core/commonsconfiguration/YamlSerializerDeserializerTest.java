package org.lable.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class YamlSerializerDeserializerTest {
    private static String testYaml;

    @BeforeClass
    public static void loadTestYaml() throws IOException {
        InputStream is = YamlSerializerDeserializer.class.getClassLoader().getResourceAsStream("test.yml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        testYaml = writer.toString();
    }

    @Test
    public void testLoad() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();
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

    @Test
    public void testSave() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationSerializer serializer = new YamlSerializerDeserializer();
        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();

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

    @Test(expected = ConfigurationException.class)
    public void testLoadBogusYaml() throws ConfigurationException, IOException, ClassNotFoundException {
        HierarchicalConfigurationDeserializer deserializer = new YamlSerializerDeserializer();
        // This won't parse.
        deserializer.deserialize(IOUtils.toInputStream("{BOGUS_YAML"));
    }
}