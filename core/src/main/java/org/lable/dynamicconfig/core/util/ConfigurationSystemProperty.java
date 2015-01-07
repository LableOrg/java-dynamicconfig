package org.lable.dynamicconfig.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * System properties relevant to the configuration process.
 */
public enum ConfigurationSystemProperty {
    /**
     * System property telling the application how it should acquire its configuration.
     */
    CONFIG_METHOD("method"),
    /**
     * System property that optionally contains the path to an alternative instance configuration.
     */
    ALTERNATE_INSTANCE_CONFIG("config"),
    /**
     * System property that optionally contains the path to a directory containing alternative instance
     * configuration files, stored under the servlet-context name.
     */
    ALTERNATE_INSTANCE_CONFIG_ROOT("configroot"),
    /**
     * System property containing a list of addresses for a Zookeeper quorum.
     */
    ZOOKEEPER_QUORUM("zookeepers"),
    /**
     * System property containing the leading part of the Zookeeper node path.
     */
    ZOOKEEPER_CONFIG_DIR("zkconfigdir"),
    /**
     * System property instructing the configuration to be retrieved from a specific znode.
     */
    ZOOKEEPER_CONFIG_NODE("zkconfignode");

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSystemProperty.class);

    /**
     * The prefix used for all configuration system properties this library uses.
     */
    static final String PROPERTY_PREFIX = "org.lable.dynamicconfig.distributedconfig";

    final String simpleName;

    ConfigurationSystemProperty(String simpleName) {
        this.simpleName = simpleName;
    }

    /**
     * Simple name of this system property.
     *
     * @return The simple name string.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * Get the fully qualified system property name for a system property.
     *
     * @param systemPropertyPrefix Prefix to prepend to the simple name.
     * @return Complete system property name.
     */
    public String propertyNameFor(String systemPropertyPrefix) {
        if (systemPropertyPrefix == null) {
            throw new IllegalArgumentException("String parameter systemPropertyPrefix cannot be null.");
        }
        return systemPropertyPrefix + "." + simpleName;
    }

    /**
     * Read all system properties relevant for the configuration of this library.
     *
     * @return A map of property values.
     */
    public static Map<ConfigurationSystemProperty, String> readConfigurationProperties() {
        Map<ConfigurationSystemProperty, String> properties = new HashMap<>();

        for (ConfigurationSystemProperty property : ConfigurationSystemProperty.values()) {
            String value = readSystemProperty(PROPERTY_PREFIX + "." + property.getSimpleName());
            properties.put(property, value);
        }
        return properties;
    }

    /**
     * Retrieve the value of a system property.
     *
     * @param name Property name.
     * @return The property value, or null if there is no such property, or if a SecurityException was thrown during
     * retrieval.
     */
    static String readSystemProperty(String name) {
        String value = null;
        try {
            value = System.getProperty(name);
        } catch (SecurityException e) {
            logger.error("Access to system property " + name + " denied by host! Skipping it.");
        }
        return value;
    }
}
