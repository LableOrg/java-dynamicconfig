package org.lable.dynamicconfig.core;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigurationInitializerIT {

    @Test
    public void noDefaultsClasspathTest() throws ConfigurationException {
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".classpath.path", "test.yml");
        Configuration configuration = ConfigurationInitializer.configureFromProperties();

        assertThat(configuration.getString("type.string"), is("Okay"));
    }

    @Test
    public void withDefaultsClasspathTest() throws ConfigurationException {
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".type", "classpath");
        System.setProperty(ConfigurationInitializer.LIBRARY_PREFIX + ".classpath.path", "test.yml");
        HierarchicalConfiguration defaults = new HierarchicalConfiguration();
        defaults.setProperty("type.string", "Not okay");
        defaults.setProperty("only.in.defaults", "XXX");

        Configuration configuration = ConfigurationInitializer.configureFromProperties(defaults);

        assertThat(configuration.getString("type.string"), is("Okay"));
        assertThat(configuration.getString("only.in.defaults"), is("XXX"));
    }
}