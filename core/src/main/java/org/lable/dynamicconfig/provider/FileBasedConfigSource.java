package org.lable.dynamicconfig.provider;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.dynamicconfig.core.ConfigChangeListener;
import org.lable.dynamicconfig.core.ConfigurationException;
import org.lable.dynamicconfig.core.commonsconfiguration.HierarchicalConfigurationDeserializer;
import org.lable.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.dynamicconfig.provider.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Loads configuration from a file on disk.
 */
public class FileBasedConfigSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedConfigSource.class);

    File config = null;

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
        return Arrays.asList("path");
    }

    /**
     * {@inheritDoc}
     * <p/>
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

        FileWatcher.Callback callback = new FileWatcher.Callback() {
            @Override
            public void fileChanged(FileWatcher.Event event, Path filePath) {
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
        Thread watcher = new Thread(fileWatcher);
        watcher.setName("File watcher, path: " + configPath);
        watcher.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(final HierarchicalConfigurationDeserializer deserializer, final ConfigChangeListener listener) {
        if (config == null) {
            logger.error("No alternative configuration file found.");
            return false;
        }

        HierarchicalConfiguration hc = loadConfiguration(config, deserializer);

        if (hc == null) {
            logger.error("Failed to load configuration file.");
            return false;
        }

        listener.changed(hc);
        return true;
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
        } catch (org.apache.commons.configuration.ConfigurationException e) {
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
