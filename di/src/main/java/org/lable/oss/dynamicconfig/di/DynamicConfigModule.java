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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationManager;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Guice dependency injection module.
 */
public class DynamicConfigModule extends AbstractModule {
    private final Logger logger = LoggerFactory.getLogger(DynamicConfigModule.class);

    @Override
    protected void configure() {
        bind(ConfigurationManager.class).toProvider(ConfigurationManagerProvider.class).in(Singleton.class);
        bind(Configuration.class).toProvider(ConfigurationProvider.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public HierarchicalConfigurationDeserializer provideDeserializer() throws ConfigurationException {
        ServiceLoader<HierarchicalConfigurationDeserializer> serviceLoader =
                ServiceLoader.load(HierarchicalConfigurationDeserializer.class);

        Iterator<HierarchicalConfigurationDeserializer> iterator = serviceLoader.iterator();
        if (!iterator.hasNext()) {
            throw new ConfigurationException("No HierarchicalConfigurationDeserializer found on the classpath. " +
                    "You may need to load a module containing a suitable deserializer.");
        }

        HierarchicalConfigurationDeserializer hierarchicalConfigurationDeserializer = iterator.next();
        logger.info("Found HierarchicalConfigurationDeserializer {}; providing it to the configuration system.",
                hierarchicalConfigurationDeserializer.getClass().getName());
        return hierarchicalConfigurationDeserializer;
    }
}
