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

import org.junit.Test;
import org.lable.oss.dynamicconfig.core.InstanceLocalSettings.MetaDataKey;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InstanceLocalSettingsTest {

    @Test
    public void setAppNameTest() {
        InstanceLocalSettings.INSTANCE.setAppName("XXX");
        assertThat(InstanceLocalSettings.INSTANCE.getAppName(), is("XXX"));
    }

    @Test
    public void getAppNameUnsetTest() {
        assertThat(InstanceLocalSettings.INSTANCE.getAppName(), is(nullValue()));
    }

    @Test
    public void setMetaData() {
        InstanceLocalSettings settings = InstanceLocalSettings.INSTANCE;
        settings.setMetaData("category1", "key1", "value1");
        settings.setMetaData("category1", "key2", "value2");

        assertThat(settings.getMetaData().size(), is(2));
        assertThat(settings.getMetaData("category1", "key1").get(), is("value1"));
        assertThat(settings.getMetaData("category1", "key2").get(), is("value2"));

        // Overwrite meta data
        settings.setMetaData("category1", "key1", "value3");

        assertThat(settings.getMetaData().size(), is(2));
        assertThat(settings.getMetaData("category1", "key1").get(), is("value3"));

        // Add new category
        settings.setMetaData("category2", "key1", "value1");

        Map<MetaDataKey, String> cat1 = settings.getMetaData("category1");
        Map<MetaDataKey, String> cat2 = settings.getMetaData("category2");
        Map<MetaDataKey, String> cat3 = settings.getMetaData("does-not-exist");

        assertThat(cat1.size(), is(2));
        assertThat(cat2.size(), is(1));
        assertThat(cat3.size(), is(0));
    }

    @Test
    public void testMetaDataKey() {
        MetaDataKey key1 = new MetaDataKey("same", "key");
        MetaDataKey key2 = new MetaDataKey("same", "key");
        MetaDataKey key3 = new MetaDataKey("different", "key");

        assertThat(key1, is(key2));
        assertThat(key1, not(key3));
        assertThat(key2, not(key3));

        assertThat(key1.toString(), is("same:key"));
        assertThat(key3.toString(), is("different:key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMetaDataKeyExceptionOnCategory() {
        new MetaDataKey(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMetaDataKeyExceptionOnKey() {
        new MetaDataKey("test", null);
    }

}