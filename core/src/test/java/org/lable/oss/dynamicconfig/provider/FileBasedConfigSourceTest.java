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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.io.File;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileBasedConfigSourceTest {

    @Test(expected = ConfigurationException.class)
    public void testConfigurationNonExistingFile() throws Exception {
        FileBasedConfigSource source = new FileBasedConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("path", "bogusPath");
        source.configure(config);
    }

    @Test
    public void testLoadConfigurationFileNotFound() throws Exception {
        // Test the handling of a FileNotFoundException thrown by FileInputStream.
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);
        File file = new File("BOGUSPATH");

        HierarchicalConfiguration result = FileBasedConfigSource.loadConfiguration(file, mockLoader);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void testLoadConfigurationUnparseable() throws Exception {
        // Test the handling of a ConfigurationException thrown by ConfigurationLoader.
        HierarchicalConfigurationDeserializer mockLoader = mock(HierarchicalConfigurationDeserializer.class);
        File configFile = File.createTempFile("configuration", ".yml");
        when(mockLoader.deserialize(any(InputStream.class))).thenThrow(new ConfigurationException("!"));

        HierarchicalConfiguration result = FileBasedConfigSource.loadConfiguration(configFile, mockLoader);

        assertThat(result, is(nullValue()));
    }
}
