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

/**
 * Passively monitor a {@link Configuration} instance for changes by comparing the timestamp of the last call to
 * {@link #modifiedSinceLastCall()} to the modification timestamp stored in the configuration under
 * {@value ConcurrentConfiguration#MODIFICATION_TIMESTAMP}. If no timestamp is stored there the current timestamp is
 * used, effectively making each call to {@link #modifiedSinceLastCall()} return true.
 * <p>
 * This class provides a way to increase efficiency of code that uses data from the configuration in computationally
 * demanding situations. Instead of recomputing this data for each call, or cashing it indefinitely, this monitor can
 * be used to determine if the configuration changed at all, and perform any operations only when this is the case.
 */
public class ConfigurationMonitor {
    final Configuration configuration;
    long timeOfLastCall;

    protected ConfigurationMonitor(Configuration configuration) {
        if (configuration == null) throw new IllegalArgumentException("Configuration passed to monitor was null.");

        this.configuration = configuration;
        // Set the time of last call to the time of modification (creation time), because
        // then the first call to #modifiedSinceLastCall will return true, which it should.
        this.timeOfLastCall = timeOfLastModification();
    }

    /**
     * Create a new {@link ConfigurationMonitor} for a {@link Configuration} instance.
     *
     * @param configuration Configuration to monitor.
     * @return A new instance of this monitor.
     */
    public static ConfigurationMonitor monitor(Configuration configuration) {
        return new ConfigurationMonitor(configuration);
    }

    /**
     * Informs whether or not the {@link Configuration} instance monitored by this monitor was changed since the last
     * call to this method. The first call to this method always return true.
     *
     * @return True if the {@link Configuration} was updated since the last call, false otherwise.
     */
    public boolean modifiedSinceLastCall() {
        long modifiedAt = timeOfLastModification();
        boolean isModified = modifiedAt >= timeOfLastCall;

        timeOfLastCall = System.nanoTime();

        return isModified;
    }

    long timeOfLastModification() {
        return configuration.getLong(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
    }
}
