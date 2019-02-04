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
package org.lable.oss.dynamicconfig.core;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.OverrideCombiner;
import org.lable.oss.dynamicconfig.core.ConfigurationComposition.ConfigReference;
import org.lable.oss.dynamicconfig.core.ConfigurationComposition.ConfigState;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;
import org.lable.oss.dynamicconfig.core.spi.HierarchicalConfigurationDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * Manages a configuration tree provided by implementations of {@link ConfigurationSource} and
 * {@link HierarchicalConfigurationDeserializer}. Provides methods that can bootstrap configuration
 * providers based on set system properties.
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    /**
     * Common prefix used for all system properties that concern this library.
     */
    public final static String LIBRARY_PREFIX = "org.lable.oss.dynamicconfig";

    /**
     * Name of the property that holds the application name.
     */
    public static final String APPNAME_PROPERTY = "appname";

    /**
     * Name of the property that holds the name of the root configuration resource.
     */
    public static final String ROOTCONFIG_PROPERTY = "rootconfig";

    static final String[] COMMON_PROPERTIES = {APPNAME_PROPERTY, ROOTCONFIG_PROPERTY};

    static ConfigurationComposition composition;

    /**
     * Use system properties to initialize a configuration instance.
     *
     * @param deserializer Deserializer used to interpret the language the configuration file is written in.
     * @return Thread-safe configuration instance.
     * @throws ConfigurationException Thrown when the required system properties are not set.
     */
    public static InitializedConfiguration configureFromProperties(HierarchicalConfigurationDeserializer deserializer)
            throws ConfigurationException {
        return configureFromProperties(null, deserializer);
    }

    /**
     * Use system properties to initialize a configuration instance.
     *
     * @param defaults     Default configuration. Any keys not overridden by the dynamic configuration will remain as
     *                     set here.
     * @param deserializer Deserializer used to interpret the language the configuration file is written in.
     * @return Thread-safe configuration instance.
     * @throws ConfigurationException Thrown when the required system properties are not set.
     */
    public static InitializedConfiguration configureFromProperties(HierarchicalConfiguration defaults,
                                                        HierarchicalConfigurationDeserializer deserializer)
            throws ConfigurationException {
        String desiredSourceName = System.getProperty(LIBRARY_PREFIX + ".type");
        if (desiredSourceName == null) {
            throw new ConfigurationException("System property " + LIBRARY_PREFIX + ".type is not set.");
        }

        ConfigurationSource desiredSource = sourceFromString(desiredSourceName);
        Configuration sourceConfiguration = gatherPropertiesFor(desiredSource, deserializer);

        return initialize(desiredSource, sourceConfiguration, deserializer, defaults);
    }

    /**
     * Initialize a configuration instance using these settings.
     *
     * @param rootConfig          Path or name of the root configuration resource.
     * @param appName             Application name.
     * @param configurationSource Configuration source.
     * @param deserializer        Deserializer.
     * @param sourceConfiguration Configuration for the {@code configurationSource} passed.
     * @param defaults            Default configuration (optional).
     * @return Thread-safe configuration instance.
     * @throws ConfigurationException Thrown when the required system properties are not set.
     */
    public static InitializedConfiguration fromTheseSettings(String rootConfig,
                                                  String appName,
                                                  ConfigurationSource configurationSource,
                                                  HierarchicalConfigurationDeserializer deserializer,
                                                  Configuration sourceConfiguration,
                                                  HierarchicalConfiguration defaults) throws ConfigurationException {

        sourceConfiguration.setProperty(ROOTCONFIG_PROPERTY, rootConfig);
        sourceConfiguration.setProperty(APPNAME_PROPERTY, appName);

        return initialize(configurationSource, sourceConfiguration, deserializer, defaults);
    }

    static InitializedConfiguration initialize(ConfigurationSource desiredSource,
                                               Configuration sourceConfiguration,
                                               HierarchicalConfigurationDeserializer deserializer,
                                               HierarchicalConfiguration defaults) throws ConfigurationException {

        if (defaults == null) defaults = new HierarchicalConfiguration();
        String rootConfigName = sourceConfiguration.getString(ROOTCONFIG_PROPERTY);

        final CombinedConfiguration allConfig = new CombinedConfiguration(new OverrideCombiner());
        final ConcurrentConfiguration concurrentConfiguration = new ConcurrentConfiguration(allConfig, desiredSource);
        composition = new ConfigurationComposition(defaults);

        logger.info("Root config: {}.", rootConfigName);
        desiredSource.configure(
                sourceConfiguration,
                defaults,
                (name, inputStream) -> {
                    logger.info("New runtime configuration received for configuration part {}.", name);
                    composition.markReferenceAsNeedsLoading(name);
                    load(name, desiredSource, deserializer, composition);
                    concurrentConfiguration.withConfiguration(composition::assembleConfigTree);
                    composition
                            .getReferences(ref -> ref.getConfigState() == ConfigState.ORPHANED)
                            .forEach(ref -> desiredSource.stopListening(ref.getName()));
                    composition.getRidOfOrphans();
                });
        String normalizedConfigName = desiredSource.normalizeRootConfigName(rootConfigName);

        ConfigReference rootReference = load(normalizedConfigName, desiredSource, deserializer, composition);
        composition.setRootReference(rootReference);

        concurrentConfiguration.withConfiguration(composition::assembleConfigTree);

        return new InitializedConfiguration(concurrentConfiguration, desiredSource);
    }

    static ConfigurationSource sourceFromString(String desiredSourceName) throws ConfigurationException {
        ServiceLoader<ConfigurationSource> loader = ServiceLoader.load(ConfigurationSource.class);
        for (ConfigurationSource source : loader) {
            if (source.name().equals(desiredSourceName)) {
                return source;
            }
        }

        throw new ConfigurationException("Could not find a ConfigurationSource with name " + desiredSourceName);
    }

    static ConfigReference load(String name,
                                ConfigurationSource desiredSource,
                                HierarchicalConfigurationDeserializer deserializer,
                                ConfigurationComposition composition) {

        // Don't try to load config parts that are already loaded.
        if (composition.hasMatchingReference(name, ref -> ref.getConfigState() != ConfigState.NEEDS_LOADING)) {
            return composition.getReference(name);
        }

        ConfigurationResult result;
        try {
            InputStream is = desiredSource.load(name);
            result = deserializer.deserialize(is);
        } catch (ConfigurationException e) {
            logger.error("Failed to (re)load (part of) configuration: {}.", name);
            return composition.markReferenceAsFailedToLoad(name);
        }

        desiredSource.listen(name);
        List<IncludeReference> includeReferences = result.getIncludeReferences();
        makeReferencesAbsolute(name, includeReferences);

        logger.info("Configuration part (re)loaded ({}).", name);
        ConfigReference reference = composition.updateReferences(name, includeReferences);
        composition.setConfigurationOnReference(reference, result.getConfiguration());

        // Recurse into every reference that was introduced here, but hasn't been loaded yet.
        composition
                .getReferences(ref -> ref.getConfigState() == ConfigState.NEEDS_LOADING)
                .forEach(ref -> load(ref.getName(), desiredSource, deserializer, composition));

        return reference;
    }

    /**
     * Make all include references 'absolute' for use in {@link ConfigurationComposition}.
     * <p>
     * For example, with {@code name} as {@code a/b/c.yaml}:
     * <ul>
     *     <li>include reference {@code ../d.yaml} becomes {@code /a/d.yaml}</li>
     *     <li>include reference {@code e.yaml} becomes {@code /a/b/f.yaml}</li>
     *     <li>include reference {@code /f.yaml} stays {@code /f.yaml}</li>
     * </ul>
     *
     * @param name              Name of the configuration resource that provides these references.
     * @param includeReferences Include references.
     */
    static void makeReferencesAbsolute(String name, List<IncludeReference> includeReferences) {
        String baseDir = name.contains("/") ? name.substring(0, name.lastIndexOf('/') + 1) : "";
        for (Iterator<IncludeReference> iterator = includeReferences.iterator(); iterator.hasNext(); ) {
            IncludeReference reference = iterator.next();
            String refName = reference.getName();
            if (refName.startsWith("/")) {
                // Already absolute. Strip the '/', it will be added at the end.
                refName = refName.substring(1);
            } else {
                refName = baseDir + refName;
            }

            refName = solveDots(refName);
            if (refName == null) {
                logger.error("Illegal configuration include reference ({}); ignored.", reference.getName());
                iterator.remove();
                continue;
            }

            reference.setName("/" + refName);
        }
    }

    static String solveDots(String path) {
        if (!path.contains(".")) {
            return path;
        }
        String[] parts = path.split("/");
        Deque<String> pathStack = new ArrayDeque<>();
        for (String part : parts) {
            switch (part) {
                case ".":
                    // Just ignore.
                    break;
                case "..":
                    if (pathStack.isEmpty()) {
                        // Trying to go beyond the root path part is illegal.
                        return null;
                    }
                    pathStack.removeLast();
                    break;
                default:
                    pathStack.addLast(part);
                    break;
            }
        }

        return String.join("/", pathStack);
    }

    static Configuration gatherPropertiesFor(ConfigurationSource desiredSource,
                                             HierarchicalConfigurationDeserializer deserializer) {
        Configuration configuration = new BaseConfiguration();

        for (String propertyName : desiredSource.systemProperties()) {
            String value = System.getProperty(LIBRARY_PREFIX + "." + desiredSource.name() + "." + propertyName);
            if (value != null && !value.equals("")) {
                configuration.setProperty(propertyName, value);
            }
        }

        // 'appname' can be (and usually is) set locally. It can be overridden by a system property for development
        // and testing purposes.
        if (InstanceLocalSettings.INSTANCE.getAppName() != null) {
            configuration.setProperty(APPNAME_PROPERTY, InstanceLocalSettings.INSTANCE.getAppName());
        }

        for (String propertyName : COMMON_PROPERTIES) {
            String value = System.getProperty(LIBRARY_PREFIX + "." + propertyName);
            if (value != null && !value.isEmpty()) {
                configuration.setProperty(propertyName, value);
            }
        }

        // If the root config name is not explicitly set, fall back to the appname.
        // If that doesn't exist, default to whatever the deserializer deems a suitable default.
        if (configuration.getString(ROOTCONFIG_PROPERTY) == null) {
            String fallback = configuration.getString(APPNAME_PROPERTY, deserializer.defaultConfigName());
            configuration.setProperty(ROOTCONFIG_PROPERTY, fallback);
        }

        return configuration;
    }

    static List<ConfigurationSource> detectConfigurationSourceServiceProviders() {
        List<ConfigurationSource> providers = new ArrayList<>();
        ServiceLoader<ConfigurationSource> loader = ServiceLoader.load(ConfigurationSource.class);
        for (ConfigurationSource source : loader) {
            providers.add(source);
        }
        return providers;
    }
}
