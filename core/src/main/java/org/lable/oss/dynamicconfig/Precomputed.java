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
package org.lable.oss.dynamicconfig;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;

import java.util.*;

/**
 * Lazy caching container for objects derived from a {@link Configuration} instance based on parameters that rarely
 * chance.
 * <p>
 * Typical usage entails invoking one of the static constructors, and calling {@link #get()} whenever the value held
 * is required. If the relevant parts of the configuration where updated since the last call, the value held will be
 * recomputed.
 *
 * @param <T> Type of the value held.
 * @see #monitorByKeys(Configuration, Precomputer, String...)
 * @see #monitorByUpdate(Configuration, Precomputer)
 */
public class Precomputed<T> {
    final Configuration configuration;
    final Precomputer<T> precomputer;
    final boolean updateOnAnyChange;
    final Map<String, ConfigSubTree> monitoredKeys;

    T precomputedValue;
    long timeOfLastUpdate;

    Precomputed(Configuration configuration,
                Precomputer<T> precomputer,
                boolean updateOnAnyChange,
                String... monitoredKeys) {

        this.configuration = configuration;
        this.precomputer = precomputer;
        this.updateOnAnyChange = updateOnAnyChange;
        this.monitoredKeys = new HashMap<>();

        for (String monitoredKey : monitoredKeys) {
            if (monitoredKey == null) throw new IllegalArgumentException("Monitored keys cannot be null.");
            this.monitoredKeys.put(monitoredKey, ConfigSubTree.forPrefix(configuration, monitoredKey));
        }

        updateValue();
    }

    /**
     * Create a new {@link Precomputed} that recomputes the value it holds when at least one of the configuration
     * parameters it monitors was changed since the last invocation.
     *
     * @param configuration Configuration to monitor.
     * @param precomputer   The computation that creates the cached value.
     * @param monitoredKeys Keys of the configuration parameters that will be compared to values they held the last
     *                      time {@link #get()} was called. These may be prefixes of configuration keys; the
     *                      configuration subtree as a whole will then be monitored.
     * @param <T>           Type of the value held.
     * @return The {@link Precomputed} instance.
     */
    public static <T> Precomputed<T> monitorByKeys(Configuration configuration,
                                                   Precomputer<T> precomputer,
                                                   String... monitoredKeys) {
        if (monitoredKeys == null || monitoredKeys.length == 0) {
            throw new IllegalArgumentException("Monitored keys are required.");
        }
        return new Precomputed<>(configuration, precomputer, false, monitoredKeys);
    }

    /**
     * Create a new {@link Precomputed} that recomputes the value it holds when the configuration is updated.
     * <p>
     * This only works as intended when the {@link Configuration} instance holds the timestamp of last modification (in
     * the form of the value returned by {@link System#nanoTime()} as parameter
     * {@value ConcurrentConfiguration#MODIFICATION_TIMESTAMP}. This is the default behaviour of Dynamic Config's
     * {@link ConcurrentConfiguration}. If this parameter is missing, the value will be recomputed each time {@link
     * #get()} is called.
     *
     * @param configuration Configuration to monitor.
     * @param precomputer   The computation that creates the cached value.
     * @param <T>           Type of the value held.
     * @return The {@link Precomputed} instance.
     */
    public static <T> Precomputed<T> monitorByUpdate(Configuration configuration,
                                                     Precomputer<T> precomputer) {
        return new Precomputed<>(configuration, precomputer, true);
    }

    /**
     * Evaluate whether the cached value held by this object needs recomputing, and return it.
     *
     * @return The result of the computation.
     */
    public T get() {
        Optional<Map<String, ConfigSubTree>> optionalConfigState = newConfigurationStateIfUpdateNeeded();
        if (optionalConfigState.isPresent()) {
            // Reflect the current state of the monitored configuration.
            monitoredKeys.replaceAll((key, configSubTree) -> optionalConfigState.get().get(key));
            updateValue();
        }

        return precomputedValue;
    }

    void updateValue() {
        precomputedValue = precomputer.compute(configuration);
        timeOfLastUpdate = System.nanoTime();
    }

    Optional<Map<String, ConfigSubTree>> newConfigurationStateIfUpdateNeeded() {
        // No need to check all monitored values if the configuration was not updated.
        if (!wasConfigUpdatedSinceLastCheck()) return Optional.empty();

        Map<String, ConfigSubTree> newConfigurationState = new HashMap<>();
        for (String key : monitoredKeys.keySet()) {
            newConfigurationState.put(key, ConfigSubTree.forPrefix(configuration, key));
        }

        if (updateOnAnyChange) {
            // When the Precomputed was instructed to update if *anything* in the configuration changes.
            return Optional.of(newConfigurationState);
        } else {
            for (Map.Entry<String, ConfigSubTree> entry : monitoredKeys.entrySet()) {
                ConfigSubTree oldConfigSubTree = entry.getValue();
                if (!oldConfigSubTree.equals(newConfigurationState.get(entry.getKey()))) {
                    // An update is needed; at least one of the monitored values differs from before.
                    return Optional.of(newConfigurationState);
                }
            }
        }

        return Optional.empty();
    }

    boolean wasConfigUpdatedSinceLastCheck() {
        long modifiedAt = configuration.getLong(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
        return modifiedAt >= timeOfLastUpdate;
    }

    /**
     * Compute a value using the supplied configuration.
     *
     * @param <T> Type of the value computed.
     */
    @FunctionalInterface
    public interface Precomputer<T> {
        /**
         * Compute a value using the supplied configuration.
         *
         * @param configuration Configuration containing parameters relevant to this {@link Precomputer}.
         * @return The computed value.
         */
        T compute(Configuration configuration);
    }

    /**
     * Represent the configuration values for a certain configuration key prefix. The main purpose if this class is
     * to efficiently ascertain whether or not any part of a configuration subtree has changed.
     */
    static class ConfigSubTree {
        private final String path;
        Map<String, Object> tree;

        private ConfigSubTree(String path, Map<String, Object> tree) {
            this.path = path;
            this.tree = tree;
        }

        public static ConfigSubTree forPrefix(Configuration configuration, String path) {
            Map<String, Object> tree = new HashMap<>();
            configuration.getKeys(path).forEachRemaining(
                    key -> tree.put(key, configuration.getProperty(key))
            );
            return new ConfigSubTree(path, tree);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, tree);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof ConfigSubTree)) return false;

            ConfigSubTree that = (ConfigSubTree) other;

            // Check if the prefix is the same.
            if (!Objects.equals(this.path, that.path)) return false;

            // First check if the amount of keys stores matches.
            if (this.tree.size() != that.tree.size()) return false;

            // Then check if the same keys are stored.
            Iterator<String> thisTreeKeys = this.tree.keySet().iterator();
            Iterator<String> thatTreeKeys = that.tree.keySet().iterator();
            for (int i = 0; i < tree.size(); i++) {
                if (!thisTreeKeys.next().equals(thatTreeKeys.next())) return false;
            }

            // Finally, compare values.
            Iterator<Map.Entry<String, Object>> thisTreeValues = this.tree.entrySet().iterator();
            Iterator<Map.Entry<String, Object>> thatTreeValues = that.tree.entrySet().iterator();
            for (int i = 0; i < tree.size(); i++) {
                if (!Objects.equals(thisTreeValues.next().getValue(), thatTreeValues.next().getValue())) return false;
            }

            return true;
        }
    }
}
