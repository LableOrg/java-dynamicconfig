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
package org.lable.oss.dynamicconfig;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.OverrideCombiner;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigChangeListener;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConfigurationMonitorTest {
    @Test
    public void monitorNoModificationTimeTest() throws InterruptedException {
        Configuration config = new BaseConfiguration();
        ConfigurationMonitor monitor = ConfigurationMonitor.monitor(config);

        // Always return true if no modification time is stored in the config.
        assertThat(monitor.modifiedSinceLastCall(), is(true));
        TimeUnit.MILLISECONDS.sleep(5);
        assertThat(monitor.modifiedSinceLastCall(), is(true));
    }

    @Test
    public void monitorTest() throws InterruptedException {
        Configuration config = new BaseConfiguration();
        config.setProperty(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
        ConfigurationMonitor monitor = ConfigurationMonitor.monitor(config);

        assertThat(monitor.modifiedSinceLastCall(), is(true));
        assertThat(monitor.modifiedSinceLastCall(), is(false));

        TimeUnit.MILLISECONDS.sleep(50);
        assertThat(monitor.modifiedSinceLastCall(), is(false));

        config.setProperty(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
        TimeUnit.MILLISECONDS.sleep(50);

        assertThat(monitor.modifiedSinceLastCall(), is(true));
        assertThat(monitor.modifiedSinceLastCall(), is(false));
    }

    @Test
    public void monitorTestWithConcurrentConfiguration() throws InterruptedException {
        final CombinedConfiguration allConfig = new CombinedConfiguration(new OverrideCombiner());
        allConfig.addConfiguration(new HierarchicalConfiguration(), "runtime");
        final ConcurrentConfiguration configuration =
                new ConcurrentConfiguration(allConfig, mock(ConfigurationSource.class));

        ConfigurationMonitor monitor = ConfigurationMonitor.monitor(configuration);

        assertThat(monitor.modifiedSinceLastCall(), is(true));
        assertThat(monitor.modifiedSinceLastCall(), is(false));

        TimeUnit.MILLISECONDS.sleep(50);
        assertThat(monitor.modifiedSinceLastCall(), is(false));

        HierarchicalConfiguration conf = new HierarchicalConfiguration();
        conf.setProperty("test", "XXX");
        configuration.updateConfiguration("runtime", conf);

        TimeUnit.MILLISECONDS.sleep(50);

        assertThat(monitor.modifiedSinceLastCall(), is(true));
        assertThat(monitor.modifiedSinceLastCall(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configurationNullTest() {
        // Should fail.
        ConfigurationMonitor.monitor(null);
    }
}