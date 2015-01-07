package org.lable.dynamicconfig.core;

import org.junit.Test;
import org.lable.dynamicconfig.core.spi.ConfigurationSource;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigurationInitializerTest {
    @Test
    public void detectServiceProvidersTest() {
        List<ConfigurationSource> result = ConfigurationInitializer.detectServiceProviders();

        assertThat(result.size(), is(2));
        assertThat(result.get(0).name(), is("file"));
        assertThat(result.get(1).name(), is("classpath"));
    }
}