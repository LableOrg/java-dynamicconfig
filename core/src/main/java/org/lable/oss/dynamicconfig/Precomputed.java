/**
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
package org.lable.oss.dynamicconfig;

import org.apache.commons.configuration.Configuration;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    final boolean updateOnChange;
    final Map<String, Object> monitoredKeys;

    T precomputedValue;
    long timeOfLastUpdate;

    Precomputed(Configuration configuration,
                Precomputer<T> precomputer,
                boolean updateOnChange,
                String... monitoredKeys) {

        this.configuration = configuration;
        this.precomputer = precomputer;
        this.updateOnChange = updateOnChange;
        this.monitoredKeys = new HashMap<>();

        for (String monitoredKey : monitoredKeys) {
            if (monitoredKey == null) throw new IllegalArgumentException("Monitored keys cannot be null.");
            this.monitoredKeys.put(monitoredKey, configuration.getProperty(monitoredKey));
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
     *                      time {@link #get()} was called.
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
        if (updateNeeded()) {
            updateValue();
        }
        return precomputedValue;
    }

    void updateValue() {
        precomputedValue = precomputer.compute(configuration);
        // Reflect the current state of the monitored configuration.
        for (Map.Entry<String, Object> entry : monitoredKeys.entrySet()) {
            monitoredKeys.put(entry.getKey(), configuration.getProperty(entry.getKey()));
        }
        timeOfLastUpdate = System.nanoTime();
    }

    boolean updateNeeded() {
        // No need to check all monitored values if the configuration was not updated.
        if (!wasConfigUpdatedSinceLastCheck()) return false;
        if (updateOnChange) return true;

        for (Map.Entry<String, Object> entry : monitoredKeys.entrySet()) {
            Object configValue = configuration.getProperty(entry.getKey());
            // An update is needed; at least one of the monitored values differs from before.
            if (!Objects.equals(configValue, entry.getValue())) return true;
        }

        return false;
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
    public interface Precomputer<T> {
        /**
         * Compute a value using the supplied configuration.
         *
         * @param configuration Configuration containing parameters relevant to this {@link Precomputer}.
         * @return The computed value.
         */
        T compute(Configuration configuration);
    }
}
