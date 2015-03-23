package org.lable.dynamicconfig.provider;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.dynamicconfig.core.ConfigurationException;
import org.lable.dynamicconfig.core.ConfigChangeListener;
import org.lable.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OnClasspathConfigSourceIT {

    @Test
    public void testLoad() throws ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        ArgumentCaptor<HierarchicalConfiguration> argument = ArgumentCaptor.forClass(HierarchicalConfiguration.class);

        OnClasspathConfigSource source = new OnClasspathConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", "testConfigOnClasspath.yml");
        source.configure(config);

        boolean result = source.load(new YamlDeserializer(), mockListener);

        assertThat(result, is(true));
        verify(mockListener).changed(argument.capture());
        assertThat(argument.getValue().getString("config.string"), is("XXX"));
    }
}
