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
package org.lable.oss.dynamicconfig.core.spi;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Service provider interface for a source of configuration information.
 * <p>
 * The general contract of this interface is that {@link #configure(Configuration, Configuration, ConfigChangeListener)}
 * is called before the two primary methods:
 * <ul>
 * <li>{@link #load(String)}
 * <li>{@link #listen(String)}
 * </ul>
 */
public interface ConfigurationSource extends Closeable {

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
     * @param changeListener Listener to inform of changes in the configuration source.
     */
    void configure(Configuration configuration, Configuration defaults, ConfigChangeListener changeListener)
            throws ConfigurationException;

    /**
     * Start listening for changes in the specified configuration part, and notify a listener of changes.
     * <p>
     * Implementing classes may or may not act on this call, depending on their nature. Static configuration sources
     * for example are presumed to never change, so no callback will ever occur for those implementations.
     *
     * @param name Configuration part name.
     */
    void listen(String name);

    /**
     * Stop listening for changes in the specified configuration part.
     *
     * @param name Configuration part name.
     */
    void stopListening(String name);

    /**
     * Load configuration from this source once.
     *
     * @param name Configuration part name.
     * @throws ConfigurationException Thrown when loading the configuration fails.
     */
    InputStream load(String name) throws ConfigurationException;

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
