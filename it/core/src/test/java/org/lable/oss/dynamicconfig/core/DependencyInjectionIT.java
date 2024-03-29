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
package org.lable.oss.dynamicconfig.core;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.di.ConfigurationDefaults;
import org.lable.oss.dynamicconfig.di.ConfigurationManagerProvider;
import org.lable.oss.dynamicconfig.di.ConfigurationProvider;
import org.lable.oss.dynamicconfig.di.DynamicConfigModule;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DependencyInjectionIT {
    @Test
    public void diProviderTest() throws IOException {
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".rootconfig", "test.yml");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(HierarchicalConfigurationDeserializer.class).to(YamlDeserializer.class);
                bind(Configuration.class).toProvider(ConfigurationProvider.class);
                bind(ConfigurationManager.class).toProvider(ConfigurationManagerProvider.class);
            }
        });

        Configuration configuration = injector.getInstance(Configuration.class);

        assertThat(configuration, is(not(nullValue())));

        ConfigurationManager configurationManager = injector.getInstance(ConfigurationManager.class);

        configurationManager.close();
    }

    @Test
    public void diProviderTestWithDefaults() {
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".rootconfig", "test.yml");
        final HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("type.string", "Not okay");
        defaults.setProperty("only.in.defaults", "XXX");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(HierarchicalConfiguration.class).annotatedWith(ConfigurationDefaults.class).toInstance(defaults);
                bind(HierarchicalConfigurationDeserializer.class).to(YamlDeserializer.class);
                bind(Configuration.class).toProvider(ConfigurationProvider.class);
                bind(ConfigurationManager.class).toProvider(ConfigurationManagerProvider.class);
            }
        });

        Configuration configuration = injector.getInstance(Configuration.class);

        // As set in test.yml:
        assertThat(configuration.getString("type.string"), is("Okay"));
        // Overridden in this test:
        assertThat(configuration.getString("only.in.defaults"), is("XXX"));
    }

    @Test
    public void diModuleTest() throws IOException {
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".rootconfig", "test.yml");

        Injector injector = Guice.createInjector(new DynamicConfigModule());

        Configuration configuration = injector.getInstance(Configuration.class);

        assertThat(configuration, is(not(nullValue())));

        ConfigurationManager configurationManager = injector.getInstance(ConfigurationManager.class);

        configurationManager.close();
    }

    @Test
    public void diModuleTestWithDefaults() {
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationLoader.LIBRARY_PREFIX + ".rootconfig", "test.yml");
        final HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("type.string", "Not okay");
        defaults.setProperty("only.in.defaults", "XXX");

        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new DynamicConfigModule());
                bind(HierarchicalConfiguration.class).annotatedWith(ConfigurationDefaults.class).toInstance(defaults);
            }
        });

        Configuration configuration = injector.getInstance(Configuration.class);

        // As set in test.yml:
        assertThat(configuration.getString("type.string"), is("Okay"));
        // Overridden in this test:
        assertThat(configuration.getString("only.in.defaults"), is("XXX"));
    }
}
