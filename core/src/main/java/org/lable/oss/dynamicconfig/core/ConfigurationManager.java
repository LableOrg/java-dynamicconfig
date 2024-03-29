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
package org.lable.oss.dynamicconfig.core;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * Contains the loaded {@link Configuration} instance, which may be acquired via {@link #getConfiguration()} and which
 * may be retained by any class using it (or injected via dependecy injection). It is thread-safe, and will be updated
 * whenever the source configuration changes.
 * <p>
 * This {@link Closeable} class may be used to shutdown the threads responsible for updating the configuration at the
 * end of an application's lifecycle. After closing it, the {@link Configuration} class should no longer be used.
 */
public class ConfigurationManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final Configuration configuration;
    private final ConfigurationLoader.ConfigLoader reloader;

    private ConfigurationConnection configurationConnection;

    public ConfigurationManager(Configuration configuration,
                                ConfigurationLoader.ConfigLoader reloader,
                                ConfigurationConnection configurationConnection) {
        this.configuration = configuration;
        this.reloader = reloader;
        this.configurationConnection = configurationConnection;
    }

    /**
     * Get the {@link Configuration} instance.
     *
     * @return A {@link Configuration} class backed by the Dynamic Config library.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Reload the configuration completely.
     */
    public void reload() throws ConfigurationException {
        try {
            close();
        } catch (IOException e) {
            logger.warn("Failed to close configuration connection properly.", e);
        }
        configurationConnection = this.reloader.connectAndLoad();
    }

    @Override
    public void close() throws IOException {
        configurationConnection.close();
    }
}
