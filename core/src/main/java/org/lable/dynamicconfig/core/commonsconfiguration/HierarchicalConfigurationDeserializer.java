package org.lable.dynamicconfig.core.commonsconfiguration;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.InputStream;

/**
 * Implementing classes can turn the serialized form of a {@link HierarchicalConfiguration} into a class instance.
 */
public interface HierarchicalConfigurationDeserializer {
    /**
     * Deserialize data representing a {@link HierarchicalConfiguration} into a class instance.
     *
     * @param input Input stream containing the raw serialized data.
     * @return A configuration instance.
     * @throws ConfigurationException Thrown when deserialization fails.
     */
    HierarchicalConfiguration deserialize(InputStream input) throws ConfigurationException;
}
