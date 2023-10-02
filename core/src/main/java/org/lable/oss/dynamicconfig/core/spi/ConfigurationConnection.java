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

import org.lable.oss.dynamicconfig.core.ConfigurationException;

import java.io.Closeable;
import java.io.InputStream;

/**
 * Provides all connections to a configuration source, including callback/listeners.
 */
public interface ConfigurationConnection extends Closeable {

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

}
