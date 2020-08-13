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
package org.lable.oss.dynamicconfig.configutil;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.lable.oss.dynamicconfig.configutil.ConfigUtil.childKeys;
import static org.lable.oss.dynamicconfig.configutil.ConfigUtil.childMap;

public class ConfigUtilTest {
    @Test
    public void testChildMapReturnsMap() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");
        input.addProperty("prop2.l1", "XXX");
        input.addProperty("prop2.l2", "XXX");
        input.addProperty("prop3.deeper", "XXX");

        final Map<String, Configuration> output = childMap(input);

        assertThat(output.size(), is(3));
        assertThat(output.get("prop3").getString("deeper"), is("XXX"));
    }

    @Test
    public void testChildMapReturnsSubsetMap() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");
        input.addProperty("prop2.l1", "XXX");
        input.addProperty("prop2.l2", "XXX");
        input.addProperty("prop3.deeper", "XXX");

        final Map<String, Configuration> output = childMap(input.subset("prop2"));

        assertThat(output.size(), is(2));
        assertThat(output.containsKey("l1"), is(true));
        assertThat(output.containsKey("l2"), is(true));
    }

    @Test
    public void testChildMapReturnsSubsetMapWithPath() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");
        input.addProperty("prop2.l1", "XXX");
        input.addProperty("prop2.l2", "XXX");
        input.addProperty("prop3.deeper", "XXX");

        final Map<String, Configuration> output = childMap(input, "prop2");

        assertThat(output.size(), is(2));
        assertThat(output.containsKey("l1"), is(true));
        assertThat(output.containsKey("l2"), is(true));
    }

    @Test
    public void testChildMapEmptyPath() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");

        final Map<String, Configuration> output = childMap(input, "");

        assertThat(output.containsKey("prop1"), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildMapRefusesNull() {
        childMap(null, null);
    }


    @Test
    public void testChildKeys() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");
        input.addProperty("prop2", "XXX");
        input.addProperty("prop3.deeper", "XXX");
        input.addProperty("prop3.deeperest", "XXX");

        final Set<String> result = childKeys(input);

        assertThat(result.size(), is(3));
        assertThat(result.contains("prop1"), is(true));
        assertThat(result.contains("prop2"), is(true));
        assertThat(result.contains("prop3"), is(true));
    }

    @Test
    public void testChildKeysSubList() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");
        input.addProperty("prop2", "XXX");
        input.addProperty("prop3.deeper", "XXX");

        final Set<String> result = childKeys(input, "prop3");

        assertThat(result.size(), is(1));
        assertThat(result.contains("deeper"), is(true));
    }

    @Test
    public void testChildKeysEmptyPath() {
        Configuration input = new HierarchicalConfiguration();
        input.addProperty("prop1", "XXX");

        final Set<String> result = childKeys(input, "");

        assertThat(result.contains("prop1"), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildKeysRefusesNull() {
        childKeys(null);
    }


    @Test
    public void codeCoverageTest() {
        new ConfigUtil();
    }
}