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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lable.oss.dynamicconfig.core.commonsconfiguration.Objectifier.traverseTreeAndEmit;

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
