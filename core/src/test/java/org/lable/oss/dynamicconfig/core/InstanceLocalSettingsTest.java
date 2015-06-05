package org.lable.oss.dynamicconfig.core;

import org.junit.Test;

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
}