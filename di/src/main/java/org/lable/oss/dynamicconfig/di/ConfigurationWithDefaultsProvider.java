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
package org.lable.oss.dynamicconfig.di;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationInitializer;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides a singleton, thread-safe configuration instance by invoking {@link
 * ConfigurationInitializer#configureFromProperties(HierarchicalConfiguration,
 * HierarchicalConfigurationDeserializer)}. It will attempt to load a configuration resource based on the system
 * properties that configure it.
 * <p/>
 * A default configuration can be specified by binding {@link ConfigurationDefaults} to an instance of
 * {@link HierarchicalConfiguration}.
 */
@Singleton
public class ConfigurationWithDefaultsProvider implements Provider<Configuration> {
    private final HierarchicalConfiguration defaults;
    final HierarchicalConfigurationDeserializer deserializer;

    @Inject
    ConfigurationWithDefaultsProvider(@ConfigurationDefaults HierarchicalConfiguration defaults,
                                      HierarchicalConfigurationDeserializer deserializer) {
        this.defaults = defaults;
        this.deserializer = deserializer;
    }

    @Override
    public Configuration get() {
        Configuration result;
        try {
            result = ConfigurationInitializer.configureFromProperties(defaults, deserializer);
        } catch (ConfigurationException e) {
            // Treat a failure to bootstrap the configuration as fatal.
            throw new RuntimeException(e);
        }
        return result;
    }
}
