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
package org.lable.oss.dynamicconfig.core;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a successful reading of a configuration source.
 */
public class ConfigurationResult {
    private final HierarchicalConfiguration configuration;
    private final List<IncludeReference> includeReferences;

    public ConfigurationResult(HierarchicalConfiguration configuration,
                               List<IncludeReference> includeReferences) {
        this.configuration = configuration;
        this.includeReferences = includeReferences == null ? new ArrayList<>() : includeReferences;
    }

    /**
     * Get the configuration tree loaded.
     *
     * @return a {@link HierarchicalConfiguration} instance.
     */
    public HierarchicalConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get the list of references to other configuration parts, if the {@link HierarchicalConfigurationDeserializer}
     * used supports it (if it doesn't, an empty list is returned).
     *
     * @return A list of references.
     */
    public List<IncludeReference> getIncludeReferences() {
        return includeReferences;
    }
}
