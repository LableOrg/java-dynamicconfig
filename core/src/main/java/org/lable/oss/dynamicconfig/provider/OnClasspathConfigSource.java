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
package org.lable.oss.dynamicconfig.provider;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Load configuration from a file on the classpath.
 */
public class OnClasspathConfigSource implements ConfigurationSource {
    /**
     * Construct a new OnClasspathConfigSource.
     */
    public OnClasspathConfigSource() {
        // Intentionally empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "classpath";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Configuration configuration, Configuration defaults, ConfigChangeListener changeListener)
            throws ConfigurationException {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream load(String name) throws ConfigurationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(name);
        if (is == null) {
            throw new ConfigurationException("Could not find " + name + " in the classpath.");
        }

        return is;
    }

    /**
     * {@inheritDoc}
     * <p>
     * OnClasspathConfigSource does not actually do anything in its implementation of this method. Classpath
     * configuration sources are presumed to be static.
     */
    @Override
    public void listen(String name) {
        // No-op. This configuration source is presumed static.
    }

    /**
     * {@inheritDoc}
     * <p>
     * OnClasspathConfigSource does not actually do anything in its implementation of this method. Classpath
     * configuration sources are presumed to be static.
     */
    @Override
    public void stopListening(String name) {
        // No-op. This configuration source is presumed static.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // No-op.
    }
}
