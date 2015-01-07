package org.lable.dynamicconfig.core.commonsconfiguration;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.OutputStream;

/**
 * Implementing classes can serialize a {@link HierarchicalConfiguration} class instance.
 */
public interface HierarchicalConfigurationSerializer {
    /**
     * Serialize a {@link HierarchicalConfiguration} class instance into its binary representation.
     *
     * @param configuration Configuration instance.
     * @param output Output stream where the serialized data will be written to.
     * @throws ConfigurationException Thrown when serialization fails.
     */
    void serialize(HierarchicalConfiguration configuration, OutputStream output) throws ConfigurationException;
}
