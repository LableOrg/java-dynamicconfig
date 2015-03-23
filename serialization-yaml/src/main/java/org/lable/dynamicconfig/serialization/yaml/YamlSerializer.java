package org.lable.dynamicconfig.serialization.yaml;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.io.IOUtils;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lable.dynamicconfig.core.commonsconfiguration.Objectifier.traverseTreeAndEmit;


/**
 * Serialize {@link org.apache.commons.configuration.HierarchicalConfiguration} instances to and from their YAML
 * representation.
 */
public class YamlSerializer implements HierarchicalConfigurationSerializer {
    private final DumperOptions yamlOptions = new DumperOptions();
    private final Yaml yaml = new Yaml(yamlOptions);

    /**
     * Construct a new YamlSerializerDeserializer.
     */
    public YamlSerializer() {
        yamlOptions.setIndent(4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(HierarchicalConfiguration configuration, OutputStream output) throws ConfigurationException {
        StringWriter writer = new StringWriter();
        yaml.dump(traverseTreeAndEmit(configuration.getRootNode()), writer);
        try {
            output.write(writer.toString().getBytes());
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }
}
