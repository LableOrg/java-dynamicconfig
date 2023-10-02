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
package org.lable.oss.dynamicconfig.provider;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lable.oss.dynamicconfig.core.ConfigurationLoader.ROOTCONFIG_PROPERTY;

/**
 * Loads configuration from a file on disk.
 */
public class FileBasedConfigSource implements ConfigurationSource {
    private Path rootDir;

    /**
     * Construct a new FileBasedConfigSource.
     */
    public FileBasedConfigSource() {
        // Intentionally empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "file";
    }

    @Override
    public void configure(Configuration configuration, Configuration defaults) throws ConfigurationException {
        String rootConfigFile = configuration.getString(ROOTCONFIG_PROPERTY);
        if (rootConfigFile == null) {
            throw new ConfigurationException("Parameter " + ROOTCONFIG_PROPERTY + " not set.");
        }

        rootDir = Paths.get(rootConfigFile).getParent();
        if (rootDir == null || !Files.isDirectory(rootDir)) {
            throw new ConfigurationException("Parameter configDir is not a directory (" + rootConfigFile + ").");
        }
    }

    @Override
    public ConfigurationConnection connect(ConfigChangeListener changeListener) throws ConfigurationException {
        return new FileBasedConfigConnection(rootDir, changeListener);
    }

    @Override
    public String normalizeRootConfigName(String rootConfigName) {
        Path path = Paths.get(rootConfigName);

        return path.getFileName().toString();
    }
}
