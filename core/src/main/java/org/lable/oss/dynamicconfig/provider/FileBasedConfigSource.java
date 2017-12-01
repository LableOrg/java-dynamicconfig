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
import org.lable.oss.dynamicconfig.provider.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lable.oss.dynamicconfig.core.ConfigurationManager.ROOTCONFIG_PROPERTY;

/**
 * Loads configuration from a file on disk.
 */
public class FileBasedConfigSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedConfigSource.class);

    ExecutorService executorService;
    Path rootDir;
    FileWatcher fileWatcher;
    ConfigChangeListener changeListener;
    Map<Path, String> pathNameMapping = new HashMap<>();

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Configuration configuration, Configuration defaults, ConfigChangeListener changeListener)
            throws ConfigurationException {
        String rootConfigFile = configuration.getString(ROOTCONFIG_PROPERTY);
        if (rootConfigFile == null) {
            throw new ConfigurationException("Parameter " + ROOTCONFIG_PROPERTY + " not set.");
        }

        this.rootDir = Paths.get(rootConfigFile).getParent();
        if (rootDir == null || !Files.isDirectory(rootDir)) {
            throw new ConfigurationException("Parameter configDir is not a directory (" + rootConfigFile + ").");
        }

        if (changeListener != null) {
            try {
                this.fileWatcher = new FileWatcher(this::handleFileChanged, rootDir);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }

            this.changeListener = changeListener;
            this.executorService = Executors.newSingleThreadExecutor();
            this.executorService.execute(fileWatcher);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream load(String name) throws ConfigurationException {
        if (name == null || name.isEmpty()) {
            throw new ConfigurationException("name", "Configuration part name cannot be null or empty.");
        }

        Path filePath = rootDir.resolve(name);
        pathNameMapping.put(filePath, name);

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to find configuration part in file " + filePath + ".", e);
        }
    }

    @Override
    public void listen(String name) {
        if (changeListener != null) {
            fileWatcher.listen(Paths.get(name));
        }
    }

    @Override
    public String normalizeRootConfigName(String rootConfigName) {
        Path path = Paths.get(rootConfigName);

        return path.getFileName().toString();
    }

    @Override
    public void stopListening(String name) {
        if (changeListener != null) {
            fileWatcher.stopListening(Paths.get(name));
        }
    }

    void handleFileChanged(FileWatcher.Event event, Path filePath) {
        switch (event) {
            case FILE_CREATED:
            case FILE_MODIFIED:
                String mutation = event == FileWatcher.Event.FILE_CREATED ? "(re)created" : "modified";
                logger.info("Configuration part file {}, reloading configuration.", mutation);
                Path relativeToRootDir = rootDir.resolve(filePath);
                String name = pathNameMapping.putIfAbsent(relativeToRootDir, relativeToRootDir.toString());
                try {
                    changeListener.changed(name, load(name));
                } catch (ConfigurationException e) {
                    logger.error("Failed to load configuration part " + name + ".", e);
                }
                break;
            case FILE_DELETED:
                logger.warn("Configuration part file {} was deleted. Its contents will be kept in configuration memory until " +
                        "a new file replaces it, or if no other configuration reference it.", filePath);
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (fileWatcher != null) fileWatcher.close();
        if (executorService != null) executorService.shutdown();
    }
}
