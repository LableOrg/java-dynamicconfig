package org.lable.dynamicconfig.serialization.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lable.dynamicconfig.core.commonsconfiguration.Objectifier.traverseTreeAndEmit;

/**
 * Serialize {@link HierarchicalConfiguration} instances into a JSON representation.
 */
public class JsonSerializer implements HierarchicalConfigurationSerializer {
    final ObjectMapper mapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(HierarchicalConfiguration configuration, OutputStream output) throws ConfigurationException {
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, traverseTreeAndEmit(configuration.getRootNode()));
            output.write(writer.toString().getBytes());
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }
}
