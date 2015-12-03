/**
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
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.io.IOUtils;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * Serialize and deserialize {@link HierarchicalConfiguration} instances to and from their YAML representation.
 */
public class YamlDeserializer implements HierarchicalConfigurationDeserializer {
    private final DumperOptions yamlOptions = new DumperOptions();
    private final Yaml yaml = new Yaml(yamlOptions);

    /**
     * Construct a new YamlSerializerDeserializer.
     */
    public YamlDeserializer() {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HierarchicalConfiguration deserialize(InputStream input) throws ConfigurationException {
        String content;
        try {
            content = IOUtils.toString(input);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read from stream.");
        }

        if (content.isEmpty()) {
            return new HierarchicalConfiguration();
        }

        Object tree;
        try {
            tree = yaml.load(content);
        } catch (ParserException e) {
            throw new ConfigurationException("Failed to parse input as valid YAML.", e);
        }
        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        traverseTreeAndLoad(configuration.getRootNode(), tree);
        return configuration;
    }

    /**
     * Process a node in the object tree, and store it with its parent node in the Config tree.
     * <p/>
     * This method recursively calls itself to walk an object tree.
     *
     * @param parent Parent of the current node, as represented in the Config tree.
     * @param node   Node to process.
     */
    void traverseTreeAndLoad(ConfigurationNode parent, Object node) {
        if (node instanceof Map<?, ?>) {
            // It is not feasible for this class to check this cast, but it is guaranteed by the
            // yaml.load() call that it is a Map<String, Object>.
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) node;

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                HierarchicalConfiguration.Node child = new HierarchicalConfiguration.Node(entry.getKey());
                child.setReference(entry);
                // Walk the complete tree.
                traverseTreeAndLoad(child, entry.getValue());
                parent.addChild(child);
            }
        } else {
            // This works for both primitives and lists.
            parent.setValue(node);
        }
    }
}
