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

import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.ConfigurationException;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationConnection;
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
import java.util.concurrent.TimeUnit;

public class FileBasedConfigConnection implements ConfigurationConnection {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedConfigConnection.class);

    private final Path rootDir;
    private final ConfigChangeListener changeListener;
    private final FileWatcher fileWatcher;
    private final ExecutorService executorService;
    private final Map<Path, String> pathNameMapping = new HashMap<>();

    public FileBasedConfigConnection(Path rootDir, ConfigChangeListener changeListener) throws ConfigurationException {
        this.rootDir = rootDir;
        this.changeListener = changeListener;

        if (changeListener != null) {
            try {
                this.fileWatcher = new FileWatcher(this::handleFileChanged, rootDir);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }

            this.executorService = Executors.newSingleThreadExecutor();
            this.executorService.execute(fileWatcher);
        } else {
            fileWatcher = null;
            executorService = null;
        }
    }

    @Override
    public void listen(String name) {
        if (changeListener != null) {
            fileWatcher.listen(Paths.get(name));
        }
    }

    @Override
    public void stopListening(String name) {
        if (changeListener != null) {
            fileWatcher.stopListening(Paths.get(name));
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

    void handleFileChanged(FileWatcher.Event event, Path filePath) {
        switch (event) {
            case FILE_CREATED:
            case FILE_MODIFIED:
                String mutation = event == FileWatcher.Event.FILE_CREATED ? "(re)created" : "modified";
                logger.info("Configuration part file {}, reloading configuration.", mutation);
                Path relativeToRootDir = rootDir.resolve(filePath);
                String name = pathNameMapping.putIfAbsent(relativeToRootDir, relativeToRootDir.toString());
                changeListener.changed(this, name);
                break;
            case FILE_DELETED:
                logger.warn("Configuration part file {} was deleted. Its contents will be kept in configuration memory until " +
                        "a new file replaces it, or if no other configuration reference it.", filePath);
                break;
            default:
                break;
        }
    }

    @Override
    public void close() throws IOException {
        if (fileWatcher != null) fileWatcher.close();
        if (executorService != null) {
            executorService.shutdown();
            try {
                boolean ignored = executorService.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
