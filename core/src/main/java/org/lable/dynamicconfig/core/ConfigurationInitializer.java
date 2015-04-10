package org.lable.dynamicconfig.core;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.OverrideCombiner;
import org.lable.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;
import org.lable.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Provides methods that can bootstrap configuration providers based on set system properties.
 */
public class ConfigurationInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationInitializer.class);

    /**
     * Common prefix used for all system properties that concern this library.
     */
    public final static String LIBRARY_PREFIX = "org.lable.dynamicconfig";

    final static String[] COMMON_PROPERTIES = {"appname"};

    /**
     * Use system properties to initialize a configuration instance.
     *
     * @param deserializer Deserializer used to interpret the language the configuration file is written in.
     * @return Thread-safe configuration instance.
     * @throws ConfigurationException
     */
    public static Configuration configureFromProperties(HierarchicalConfigurationDeserializer deserializer)
            throws ConfigurationException {
        return configureFromProperties(null, deserializer);
    }

    /**
     * Use system properties to initialize a configuration instance.
     *
     * @param defaults     Default configuration. Any keys not overridden by the dynamic configuration will remain as
     *                     set here.
     * @param deserializer Deserializer used to interpret the language the configuration file is written in.
     * @return Thread-safe configuration instance.
     * @throws ConfigurationException
     */
    public static Configuration configureFromProperties(HierarchicalConfiguration defaults,
                                                        HierarchicalConfigurationDeserializer deserializer)
            throws ConfigurationException {
        String desiredSourceName = System.getProperty(LIBRARY_PREFIX + ".type");
        if (desiredSourceName == null) {
            throw new ConfigurationException("System property " + LIBRARY_PREFIX + ".type is not set.");
        }

        List<ConfigurationSource> sources = detectConfigurationSourceServiceProviders();
        ConfigurationSource desiredSource = null;
        for (ConfigurationSource source : sources) {
            if (source.name().equals(desiredSourceName)) {
                desiredSource = source;
                break;
            }
        }

        if (desiredSource == null) {
            throw new ConfigurationException("Could not find a ConfigurationSource with name " + desiredSourceName);
        }

        Configuration sourceConfiguration = gatherPropertiesFor(desiredSource);
        desiredSource.configure(sourceConfiguration);

        // Create the configuration object with its defaults loaded last. The combiner expects them in that order.
        final CombinedConfiguration allConfig = new CombinedConfiguration(new OverrideCombiner());
        final ConcurrentConfiguration concurrentConfiguration = new ConcurrentConfiguration(allConfig);

        // Add an empty named placeholder for the runtime configuration that will be loaded later on.
        allConfig.addConfiguration(new HierarchicalConfiguration(), "runtime");
        if (defaults != null) {
            allConfig.addConfiguration(defaults, "defaults");
        }

        // Listens to changes in the configuration source and updates the configuration tree.
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void changed(HierarchicalConfiguration fresh) {
                logger.info("New runtime configuration received.");
                concurrentConfiguration.updateConfiguration("runtime", fresh);
            }
        };

        boolean successfullyLoaded = desiredSource.load(deserializer, listener);

        if (!successfullyLoaded) {
            throw new ConfigurationException("Failed to load configuration.");
        }

        // Listen for future changes in the run-time configuration.
        desiredSource.listen(deserializer, listener);

        return concurrentConfiguration;
    }

    static Configuration gatherPropertiesFor(ConfigurationSource desiredSource) {
        Configuration configuration = new BaseConfiguration();

        for (String propertyName : desiredSource.systemProperties()) {
            String value = System.getProperty(LIBRARY_PREFIX + "." + desiredSource.name() + "." + propertyName);
            if (value != null && !value.equals("")) {
                configuration.setProperty(propertyName, value);
            }
        }
        for (String propertyName : COMMON_PROPERTIES) {
            String value = System.getProperty(LIBRARY_PREFIX + "." + propertyName);
            if (value != null && !value.equals("")) {
                configuration.setProperty(propertyName, value);
            }
        }

        return configuration;
    }

    static List<ConfigurationSource> detectConfigurationSourceServiceProviders() {
        List<ConfigurationSource> providers = new ArrayList<>();
        ServiceLoader<ConfigurationSource> loader = ServiceLoader.load(ConfigurationSource.class);
        for (ConfigurationSource source : loader) {
            providers.add(source);
        }
        return providers;
    }

    static List<HierarchicalConfigurationDeserializer> detectDeserializationServiceProviders() {
        List<HierarchicalConfigurationDeserializer> providers = new ArrayList<>();
        ServiceLoader<HierarchicalConfigurationDeserializer> loader =
                ServiceLoader.load(HierarchicalConfigurationDeserializer.class);
        for (HierarchicalConfigurationDeserializer source : loader) {
            providers.add(source);
        }
        return providers;
    }
}
