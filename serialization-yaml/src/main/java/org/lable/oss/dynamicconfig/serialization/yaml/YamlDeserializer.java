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
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.io.IOUtils;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationResult;
import org.lable.oss.dynamicconfig.core.IncludeReference;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.serialization.yaml.snake.CustomConstructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * Serialize and deserialize {@link HierarchicalConfiguration} instances to and from their YAML representation.
 */
public class YamlDeserializer implements HierarchicalConfigurationDeserializer {
    private final Yaml yaml;

    /**
     * Construct a new YamlSerializerDeserializer.
     */
    public YamlDeserializer() {
        DumperOptions yamlOptions = new DumperOptions();
        Representer representer = new Representer();
        CustomConstructor customConstructor = new CustomConstructor();

        yaml = new Yaml(customConstructor, representer, yamlOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationResult deserialize(InputStream input) throws ConfigurationException {
        String content = new Scanner(input).useDelimiter("\\A").next();

        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        List<IncludeReference> includes = new ArrayList<>();
        if (!content.isEmpty()) {
            Object tree;
            try {
                tree = yaml.load(content);
            } catch (ParserException e) {
                throw new ConfigurationException("Failed to parse input as valid YAML.", e);
            }
            traverseTreeAndLoad(configuration.getRootNode(), null, includes, tree);

            // Get the references from the special 'extends' key.
            for (String reference : configuration.getStringArray("extends")) {
                includes.add(new IncludeReference(reference));
            }
        }

        return new ConfigurationResult(configuration, includes);
    }

    @Override
    public String defaultConfigName() {
        return "config.yaml";
    }

    /**
     * Process a node in the object tree, and store it with its parent node in the Config tree.
     * <p>
     * This method recursively calls itself to walk an object tree.
     *
     * @param parent   Parent of the current node, as represented in the Config tree.
     * @param path     Path.
     * @param includes Includes encountered.
     * @param node     Node to process.
     */
    void traverseTreeAndLoad(ConfigurationNode parent, String path, List<IncludeReference> includes, Object node) {
        if (node instanceof IncludeReference) {
            IncludeReference include = (IncludeReference) node;
            include.setConfigPath(path);
            includes.add(include);
        } else if (node instanceof Map<?, ?>) {
            // It is not feasible for this class to check this cast, but it is guaranteed by the
            // yaml.load() call that it is a Map<String, Object>.
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) node;

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                HierarchicalConfiguration.Node child = new HierarchicalConfiguration.Node(key);
                child.setReference(entry);
                // Walk the complete tree.
                traverseTreeAndLoad(child, combineConfigKeyPath(path, key), includes, entry.getValue());
                parent.addChild(child);
            }
        } else {
            // This works for both primitives and lists.
            parent.setValue(node);
        }
    }

    static String combineConfigKeyPath(String prefix, String name) {
        return prefix == null ? name : prefix + "." + name;
    }
}
