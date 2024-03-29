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
package org.lable.oss.dynamicconfig.di;

import com.google.inject.Inject;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationLoader;
import org.lable.oss.dynamicconfig.core.ConfigurationManager;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides a singleton, thread-safe configuration manager instance by invoking {@link
 * ConfigurationLoader#configureFromProperties(HierarchicalConfiguration,
 * HierarchicalConfigurationDeserializer)}. It will attempt to load a configuration resource based on the system
 * properties that configure it.
 * <p>
 * A default configuration can be specified by binding {@link ConfigurationDefaults} to an instance of
 * {@link HierarchicalConfiguration}.
 */
@Singleton
public class ConfigurationManagerProvider implements Provider<ConfigurationManager> {
    final HierarchicalConfigurationDeserializer deserializer;
    HierarchicalConfiguration defaults;
    String sourceType;
    ConfigurationManagerCloser closer;

    @Inject
    ConfigurationManagerProvider(HierarchicalConfigurationDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Inject(optional = true)
    public void setDefaults(@ConfigurationDefaults HierarchicalConfiguration defaults) {
        this.defaults = defaults;
    }

    @Inject(optional = true)
    public void setSourceType(@ConfigurationSourceType String sourceType) {
        this.sourceType = sourceType;
    }

    @Inject(optional = true)
    public void setCloser(ConfigurationManagerCloser closer) {
        this.closer = closer;
    }

    @Override
    public ConfigurationManager get() {
        try {
            ConfigurationManager configurationManager = ConfigurationLoader.configureFromProperties(
                    sourceType, defaults, deserializer
            );
            if (closer != null) {
                closer.register(configurationManager);
            }
            return configurationManager;
        } catch (ConfigurationException e) {
            // Treat a failure to bootstrap the configuration as fatal.
            throw new RuntimeException(e);
        }
    }
}
