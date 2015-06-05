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
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.util.List;

import static org.lable.oss.dynamicconfig.core.ConfigurationInitializer.detectConfigurationSourceServiceProviders;
import static org.lable.oss.dynamicconfig.core.ConfigurationInitializer.detectDeserializationServiceProviders;

public class ListSPIImplemenations {
    @Test
    public void listSPIImplementationsTest() {
        List<ConfigurationSource> configurationSources = detectConfigurationSourceServiceProviders();
        System.out.println("Detected configuration sources:");
        for (ConfigurationSource configurationSource : configurationSources) {
            System.out.println("  " + configurationSource.name() +
                    " (" + configurationSource.getClass().getCanonicalName() + ")");
        }

        List<HierarchicalConfigurationDeserializer> deserializers = detectDeserializationServiceProviders();
        System.out.println("Detected deserializers:");
        for (HierarchicalConfigurationDeserializer deserializer : deserializers) {
            System.out.println("  " + deserializer.getClass().getCanonicalName());
        }
    }
}
