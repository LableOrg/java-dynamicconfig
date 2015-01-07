package org.lable.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

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
            mapper.writeValue(writer, traverseTreeAndSave(configuration.getRootNode()));
            output.write(writer.toString().getBytes());
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Process a node in the Config tree, and store it with its parent node in an object tree.
     * <p/>
     * This method recursively calls itself to walk a Config tree.
     *
     * @param parent Parent of the current node, as represented in the Config tree.
     * @return An object tree.
     */
    Object traverseTreeAndSave(ConfigurationNode parent) {
        if (parent.getChildrenCount() == 0) {
            return parent.getValue();
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Object o : parent.getChildren()) {
                ConfigurationNode child = (ConfigurationNode) o;
                String nodeName = child.getName();
                map.put(nodeName, traverseTreeAndSave(child));
            }
            return map;
        }
    }
}
