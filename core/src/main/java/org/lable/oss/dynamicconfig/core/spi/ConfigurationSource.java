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
package org.lable.oss.dynamicconfig.core.spi;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;

import java.io.Closeable;
import java.util.List;

/**
 * Service provider interface for a source of configuration information.
 * <p>
 * The general contract of this interface is that its methods are called in this order:
 * <ul>
 *     <li>{@link #configure(Configuration)}
 *     <li>{@link #load(HierarchicalConfigurationDeserializer, ConfigChangeListener)}
 *     <li>{@link #listen(HierarchicalConfigurationDeserializer, ConfigChangeListener)}
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
    List<String> systemProperties();

    /**
     * Provide configuration parameters.
     *
     * @param configuration Configuration parameters for this implementation.
     */
    void configure(Configuration configuration) throws ConfigurationException;

    /**
     * Start listening for changes in the configuration source, and notify a listener of changes in the configuration
     * source.
     * <p>
     * Implementing classes may or may not act on this call, depending on their nature. Static configuration sources
     * for example are presumed to never change, so no callback will ever occur for those implementations.
     *
     * @param deserializer Deserializer that can turn an input stream into a configuration instance.
     * @param listener Listener to inform of changes in the configuration source.
     */
    void listen(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener);

    /**
     * Load configuration from this source once.
     *
     * @param deserializer Deserializer that can turn an input stream into a configuration instance.
     * @param listener Listener to notify when the configuration has been loaded.
     * @throws ConfigurationException Thrown when loading the configuration fails.
     */
    void load(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener)
            throws ConfigurationException;
}
