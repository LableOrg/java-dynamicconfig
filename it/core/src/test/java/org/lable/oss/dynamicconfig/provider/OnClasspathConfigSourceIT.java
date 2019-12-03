/*
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
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.ConfigurationResult;
import org.lable.oss.dynamicconfig.serialization.yaml.YamlDeserializer;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OnClasspathConfigSourceIT {
//
//    @Test
//    public void testLoad() throws ConfigurationException {
//        ConfigChangeListener mockListener = mock(ConfigChangeListener.class);
//        ArgumentCaptor<ConfigurationResult> argument = ArgumentCaptor.forClass(ConfigurationResult.class);
//
//        OnClasspathConfigSource source = new OnClasspathConfigSource();
//        Configuration config = new BaseConfiguration();
//        config.setProperty("path", "testConfigOnClasspath.yml");
//        source.configure(config);
//
//        source.load(new YamlDeserializer(), mockListener);
//
//        verify(mockListener).changed("testConfigOnClasspath.yml", argument.capture());
//        assertThat(argument.getValue().getConfiguration().getString("config.string"), is("XXX"));
//    }
}
