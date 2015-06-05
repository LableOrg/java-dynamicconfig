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

import org.junit.Test;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigurationInitializerTest {
    @Test
    public void detectServiceProvidersTest() {
        List<ConfigurationSource> result = ConfigurationInitializer.detectConfigurationSourceServiceProviders();

        assertThat(result.size(), is(2));
        assertThat(result.get(0).name(), is("file"));
        assertThat(result.get(1).name(), is("classpath"));
    }
}