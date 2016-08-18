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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.lable.oss.dynamicconfig.provider.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Loads configuration from a file on disk.
 */
public class FileBasedConfigSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedConfigSource.class);

    File config = null;

    Runnable watcher;
    ExecutorService executorService;

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
    public List<String> systemProperties() {
        return Collections.singletonList("path");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Expects a parameter "path" containing the path to the configuration file, relative to the classpath.
     */
    @Override
    public void configure(Configuration configuration) throws ConfigurationException {
        String configPath = configuration.getString("path");

        if (isBlank(configPath)) {
            throw new ConfigurationException("path", "Parameter not set or empty.");
        }

        File file = new File(configPath);
        if (!file.exists()) {
            throw new ConfigurationException("path", format("File does not exist at path %s.", file.getPath()));
        }

        config = file;
        logger.info("Loading configuration from file: " + config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void listen(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener) {
        Path configPath = config.toPath();

        FileWatcher.Callback callback = (event, filePath) -> {
            HierarchicalConfiguration hc = null;
            switch (event) {
                case FILE_MODIFIED:
                    logFileModified(filePath);
                    hc = loadConfiguration(filePath.toFile(), deserializer);
                    break;
                case FILE_CREATED:
                    logFileCreated(filePath);
                    // There is no need to reload the configuration after FILE_CREATED, because FILE_MODIFIED
                    // will be raised right after it.
                    break;
                case FILE_DELETED:
                    logFileDeleted(filePath);
                    break;
            }
            if (hc != null) {
                listener.changed(hc);
            }
        };

        FileWatcher fileWatcher;
        try {
            fileWatcher = new FileWatcher(callback, configPath);
        } catch (IOException e) {
            logger.error(format("Failed to acquire a file watcher. " +
                    "Configuration file %s will not be monitored for changes.", configPath));
            return;
        }

        // Launch the file watcher.
        watcher = fileWatcher;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(fileWatcher);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener)
            throws ConfigurationException {
        if (config == null) {
            throw new ConfigurationException("No alternative configuration file found.");
        }

        HierarchicalConfiguration hc = loadConfiguration(config, deserializer);

        if (hc == null) {
            throw new ConfigurationException("Failed to load configuration file.");
        }

        listener.changed(hc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        executorService.shutdown();
    }

    static HierarchicalConfiguration loadConfiguration(File config, HierarchicalConfigurationDeserializer deserializer) {
        InputStream is;
        try {
            is = new FileInputStream(config);
        } catch (FileNotFoundException e) {
            logger.warn("Tried to load configuration at: " + config.getAbsolutePath()
                    + ", but no file could be found at that path.");
            return null;
        }

        HierarchicalConfiguration hc;
        try {
            hc = deserializer.deserialize(is);
        } catch (ConfigurationException e) {
            logger.error("Failed to parse supplied configuration file.", e);
            return null;
        }

        return hc;
    }

    static void logFileModified(Path filePath) {
        logger.info(format("Configuration file %s modified, reloading configuration.", filePath));
    }

    static void logFileDeleted(Path filePath) {
        logger.warn(format("Configuration file %s was deleted. Keeping currently loaded configuration.", filePath));
    }

    static void logFileCreated(Path filePath) {
        logger.info(format("Configuration file %s (re)created.", filePath));
    }
}
