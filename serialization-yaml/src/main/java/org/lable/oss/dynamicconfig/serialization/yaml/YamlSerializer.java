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
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import static org.lable.oss.dynamicconfig.core.commonsconfiguration.Objectifier.traverseTreeAndEmit;


/**
 * Serialize {@link org.apache.commons.configuration.HierarchicalConfiguration} instances to and from their YAML
 * representation.
 */
public class YamlSerializer implements HierarchicalConfigurationSerializer {
    private final Yaml yaml;

    /**
     * Construct a new YamlSerializerDeserializer.
     */
    public YamlSerializer() {
        DumperOptions yamlOptions = new DumperOptions();
        yamlOptions.setIndent(4);
        // Improves readability by omitting {} where possible, and using indented blocks instead.
        yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(yamlOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(HierarchicalConfiguration configuration, OutputStream output, boolean humanReadable)
            throws ConfigurationException {
        // Ignore the humanReadable flag; YAML is human-readable by default. :)
        StringWriter writer = new StringWriter();
        yaml.dump(traverseTreeAndEmit(configuration.getRootNode()), writer);
        try {
            output.write(writer.toString().getBytes());
        } catch (IOException e) {
            throw new ConfigurationException("IOException caught.", e);
        }
    }
}
