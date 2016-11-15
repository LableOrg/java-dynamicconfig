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
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.Precomputed.ConfigSubTree;
import org.lable.oss.dynamicconfig.core.commonsconfiguration.ConcurrentConfiguration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class PrecomputedTest {
    @Test
    public void basicTest() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty("a", "XXX");
        configuration.setProperty("b", "YYY");
        configuration.setProperty("c", 1);
        Precomputed<String> precomputed = Precomputed.monitorByKeys(
                configuration,
                config -> config.getString("a") + "--" + config.getString("b") + "--" + config.getInt("c", 0),
                "a", "b");

        assertThat(precomputed.get(), is("XXX--YYY--1"));

        // Not a monitored value, so no update.
        configuration.setProperty("c", 2);
        assertThat(precomputed.get(), is("XXX--YYY--1"));

        // Monitored value; update.
        configuration.setProperty("a", "ZZZ");
        assertThat(precomputed.get(), is("ZZZ--YYY--2"));

        // Monitored value; update.
        configuration.setProperty("b", "XXX");
        assertThat(precomputed.get(), is("ZZZ--XXX--2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refuseNullKeys() {
        Precomputed.monitorByKeys(new BaseConfiguration(), configuration -> null, "a", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refuseNoKeys() {
        Precomputed.monitorByKeys(new BaseConfiguration(), configuration -> null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refuseNullAsKeyList() {
        Precomputed.monitorByKeys(new BaseConfiguration(), configuration -> null, (String[]) null);
    }

    @Test
    public void timestampTest() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty("a", "AAA");
        configuration.setProperty(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
        Precomputed<String> precomputed = Precomputed.monitorByUpdate(
                configuration,
                config -> config.getString("a")
        );

        assertThat(precomputed.get(), is("AAA"));

        // Not a monitored value, so no update.
        configuration.setProperty("a", "BBB");
        assertThat(precomputed.get(), is("AAA"));

        // Touch the timestamp so an update will be required.
        configuration.setProperty(ConcurrentConfiguration.MODIFICATION_TIMESTAMP, System.nanoTime());
        assertThat(precomputed.get(), is("BBB"));
    }

    @Test
    public void nullTest() {
        Configuration configuration = new BaseConfiguration();
        Precomputed<String> precomputed = Precomputed.monitorByKeys(
                configuration,
                config -> String.valueOf(config.getString("a")) + "-" +
                        String.valueOf(config.getString("b")),
                "a", "b"
        );

        assertThat(precomputed.get(), is("null-null"));

        configuration.setProperty("a", "XXX");
        configuration.setProperty("b", "YYY");
        assertThat(precomputed.get(), is("XXX-YYY"));

        configuration.setProperty("a", null);
        assertThat(precomputed.get(), is("null-YYY"));

        //configuration.setProperty("a", "AAA");
        configuration.setProperty("b", null);
        assertThat(precomputed.get(), is("null-null"));

        configuration.setProperty("b", "XXX");
        assertThat(precomputed.get(), is("null-XXX"));
    }

    @Test
    public void subtreeTest() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty("a.1", 1);
        configuration.setProperty("a.2", "AAA2");
        configuration.setProperty("b.1", "BBB");
        Precomputed<Integer> precomputed = Precomputed.monitorByKeys(
                configuration,
                config -> config.getInt("a.1") + 1,
                "a"
        );

        assertThat(precomputed.get(), is(2));
        assertThat(precomputed.get(), is(2));

        configuration.setProperty("a.1", 4);

        assertThat(precomputed.get(), is(5));

        configuration.setProperty("b.1", "BBB~");

        assertThat(precomputed.get(), is(5));

        configuration.setProperty("a.2", "AAA2~");

        assertThat(precomputed.get(), is(5));
    }

    @Test
    public void configSubTreeEqualityTest() {
        Configuration conf = new BaseConfiguration();
        conf.addProperty("a.1", "XXX");
        conf.addProperty("a.2", 123);

        ConfigSubTree treeA = ConfigSubTree.forPrefix(conf, "a");
        ConfigSubTree treeB = ConfigSubTree.forPrefix(conf, "a");
        ConfigSubTree treeC = ConfigSubTree.forPrefix(conf, "b");
        ConfigSubTree treeD = ConfigSubTree.forPrefix(conf, "a.1");

        assertThat(treeA, is(treeB));
        assertThat(treeA.tree.size(), is(2));
        assertThat(treeA, is(not(treeC)));
        assertThat(treeC.tree.size(), is(0));
        assertThat(treeA, is(not(treeD)));
        assertThat(treeD.tree.size(), is(1));

        conf.addProperty("a.3", true);
        ConfigSubTree treeE = ConfigSubTree.forPrefix(conf, "a");

        assertThat(treeA, is(not(treeE)));

        // Change a property.
        conf.setProperty("a.1", "ZZZ");
        ConfigSubTree treeF = ConfigSubTree.forPrefix(conf, "a");

        assertThat(treeA, is(not(treeF)));

        // Reset the configuration to its original state.
        conf.setProperty("a.1", "XXX");
        conf.clearProperty("a.3");
        ConfigSubTree treeG = ConfigSubTree.forPrefix(conf, "a");

        assertThat(treeA, is(treeG));
    }
}