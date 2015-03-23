package org.lable.dynamicconfig.core;

import org.junit.Test;
import org.lable.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;

import java.util.List;

import static org.lable.dynamicconfig.core.ConfigurationInitializer.detectConfigurationSourceServiceProviders;
import static org.lable.dynamicconfig.core.ConfigurationInitializer.detectDeserializationServiceProviders;

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
