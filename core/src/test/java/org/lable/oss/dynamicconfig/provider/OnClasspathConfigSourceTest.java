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
package org.lable.oss.dynamicconfig.provider;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OnClasspathConfigSourceTest {

    @Test
    public void testListen() {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);

        // Not implemented, so the only thing we can test is whether the method can be called.
        OnClasspathConfigSource source = new OnClasspathConfigSource();
        source.listen(mockLoader, mockListener);
    }

    @Test
    public void testLoadNoResource() throws org.lable.oss.dynamicconfig.core.ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);

        OnClasspathConfigSource source = new OnClasspathConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", "bogusPath");
        source.configure(config);

        boolean result = source.load(mockLoader, mockListener);

        assertThat(result, is(false));
    }

    @Test
    public void testLoadFailedLoadConfig()
            throws ConfigurationException, org.lable.oss.dynamicconfig.core.ConfigurationException {
        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);

        when(mockLoader.deserialize(any(InputStream.class))).thenThrow(new ConfigurationException("!"));

        OnClasspathConfigSource source = new OnClasspathConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", "dummy.txt");
        source.configure(config);
        boolean result = source.load(mockLoader, mockListener);

        assertThat(result, is(false));
        verify(mockLoader, times(1)).deserialize(any(InputStream.class));
    }
}
