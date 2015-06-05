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
package org.lable.oss.dynamicconfig.core;


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BasicUseIT {
    @Test
    public void noDefaultsClasspathTest() throws ConfigurationException {
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".classpath.path", "test.yml");
        Configuration configuration = ConfigurationInitializer.configureFromProperties(
                new YamlDeserializer()
        );

        assertThat(configuration.getString("type.string"), is("Okay"));
    }

    @Test
    public void withDefaultsClasspathTest() throws ConfigurationException {
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".classpath.path", "test.yml");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("type.string", "Not okay");
        defaults.setProperty("only.in.defaults", "XXX");

        Configuration configuration = ConfigurationInitializer.configureFromProperties(
                defaults, new YamlDeserializer()
        );

        assertThat(configuration.getString("type.string"), is("Okay"));
        assertThat(configuration.getString("only.in.defaults"), is("XXX"));
    }
}
