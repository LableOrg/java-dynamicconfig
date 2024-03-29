/*
 * Copyright © 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.core.spi;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;

import java.util.Collections;
import java.util.List;

/**
 * Service provider interface for a source of configuration information.
 * <p>
 * The general contract of this interface is that {@link #configure(Configuration, Configuration)}
 * is called before {@link #connect(ConfigChangeListener)}.
 */
public interface ConfigurationSource {

    /**
     * The simple name of an implementation of {@link ConfigurationSource} should be unique. It is used to link
     * system properties relevant to the configuration of this class to it.
     *
     * @return Simple name of this configuration source.
     */
    String name();

    /**
     * A set of all system properties relevant to this configuration source for bootstrapping itself.
     *
     * @return A list of property names, without the common library prefix.
     */
    default List<String> systemProperties() {
        return Collections.emptyList();
    }

    /**
     * Provide configuration parameters.
     *
     * @param configuration  Configuration parameters for this implementation.
     * @param defaults       Default settings. Implementing classes may add configuration parameters to this.
     */
    void configure(Configuration configuration, Configuration defaults)
            throws ConfigurationException;

    ConfigurationConnection connect(ConfigChangeListener changeListener) throws ConfigurationException;

    /**
     * Turn the root config name into its corresponding configuration part name. E.g., for a file based configuration
     * source the root config name the application is bootstrapped with might be an absolute URL like
     * {@code /home/user/projects/application/config/config.yaml}, the normalized name would be {@code config.yaml}.
     *
     * @param rootConfigName Root config name.
     * @return The normalized name.
     */
    default String normalizeRootConfigName(String rootConfigName) {
        return rootConfigName;
    }
}
